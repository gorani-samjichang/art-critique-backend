package com.gorani_samjichang.art_critique.feedback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorani_samjichang.art_critique.appConstant.FeedbackState;
import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.common.exceptions.BadFeedbackRequestException;
import com.gorani_samjichang.art_critique.common.exceptions.CannotFindBySerialNumberException;
import com.gorani_samjichang.art_critique.common.exceptions.NoPermissionException;
import com.gorani_samjichang.art_critique.common.exceptions.RegenerationFeedbackNotFoundException;
import com.gorani_samjichang.art_critique.common.exceptions.UserNotFoundException;
import com.gorani_samjichang.art_critique.credit.CreditEntity;
import com.gorani_samjichang.art_critique.credit.CreditRepository;
import com.gorani_samjichang.art_critique.credit.CreditUsedHistoryEntity;
import com.gorani_samjichang.art_critique.credit.CreditUsedHistoryRepository;
import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import com.gorani_samjichang.art_critique.study.ExternalStudyContentsRepository;
import com.gorani_samjichang.art_critique.study.StudyInfoDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private static final int PAGE_SIZE = 4;
    final MemberRepository memberRepository;
    final CreditRepository creditRepository;
    final CreditUsedHistoryRepository creditUsedHistoryRepository;
    final CommonUtil commonUtil;
    final BCryptPasswordEncoder bCryptPasswordEncoder;
    final WebClient.Builder webClientBuilder;
    @Value("${feedback.server.host}")
    private String feedbackServerHost;
    final EmitterService emitterService;
    final CommentRepository commentRepository;
    private List<FeedbackUrlDto> goodImages = new ArrayList<>();
    final ExternalStudyContentsRepository externalStudyContentsRepository;

    @Scheduled(fixedRate = 1000 * 60 * 15)
    public void checkGoodImages() {
        goodImages = feedbackRepository.findGoodImage();
    }

    List<FeedbackUrlDto> getGoodImage() {
        if (goodImages.size() < 10) {
            checkGoodImages();
        }
        return goodImages;
    }

    public ResponseEntity<String> requestFeedback(
            MultipartFile imageFile,
            CustomUserDetails userDetails) throws IOException, UserNotFoundException, RegenerationFeedbackNotFoundException {
        return requestFeedback(imageFile, userDetails, null);
    }

    public ResponseEntity<String> requestFeedback(
            String feedbackSerialNumber,
            CustomUserDetails userDetails) throws IOException, UserNotFoundException, RegenerationFeedbackNotFoundException {
        return requestFeedback(null, userDetails, feedbackSerialNumber);
    }

    public ResponseEntity<String> requestFeedback(
            MultipartFile imageFile,
            CustomUserDetails userDetails,
            String regenerationFeedbackSerialNumber) throws IOException, UserNotFoundException, RegenerationFeedbackNotFoundException {
        MemberEntity me = memberRepository.findById(userDetails.getUid()).orElseThrow(() -> new UserNotFoundException("user not found"));
        CreditEntity usedCredit = creditRepository.usedCreditEntityByRequest(userDetails.getUid());
        if (me.getCredit() <= 0 || usedCredit == null) {
            return new ResponseEntity<>("noCredit", HttpStatusCode.valueOf(200));
        }

        FeedbackEntity oldFeedbackEntity = null;
        if (regenerationFeedbackSerialNumber != null) {
            oldFeedbackEntity = feedbackRepository.findBySerialNumber(regenerationFeedbackSerialNumber).orElseThrow(() -> new RegenerationFeedbackNotFoundException("Invalid regeneration feedback serial number"));
            if(!oldFeedbackEntity.getMemberEntity().getSerialNumber().equals(userDetails.getSerialNumber())) {
                throw new NoPermissionException("You are not allowed to re-request.");
           }
        }

        String serialNumber = UUID.randomUUID().toString();
        String imageUrl;
        if (oldFeedbackEntity == null) {
            String pictureUUID = UUID.randomUUID().toString();
            imageUrl = commonUtil.uploadToStorage(imageFile, pictureUUID);
        } else {
            imageUrl = oldFeedbackEntity.getPictureUrl();
        }
        FeedbackEntity.FeedbackEntityBuilder feedbackEntityBuilder = FeedbackEntity
                .builder()
                .serialNumber(serialNumber)
                .state("NOT_STARTED")
                .progressRate(0)
                .isBookmarked(false)
                .isPublic(true)
                .pictureUrl(imageUrl)
                .isHead(true)
                .tail(null);
        if (oldFeedbackEntity != null) {
            feedbackEntityBuilder = feedbackEntityBuilder
                    .isHead(false)
                    .tail(oldFeedbackEntity.getSerialNumber());
        }
        FeedbackEntity feedbackEntity = feedbackEntityBuilder.build();

        me.addFeedback(feedbackEntity);
        usedCredit.useCredit();

        creditRepository.save(usedCredit);
        feedbackRepository.save(feedbackEntity);
        memberRepository.save(me);
        
        // progressRate가 1초마다 1씩 증가
        // 피드백 서버의 요청이 끝나지 않았을 때 progressRate가 49까지 증가
        // 피드백 서버의 요청이 끝나면 progressRate가 50으로 설정
        // 이후 progressRate가 1초마다 1씩 증가
        // 추천 학습 자료 추가 끝나지 않았을 때 progressRate가 99까지 증가
        // 추천 학습 자료 추가가 끝나면 progressRate가 100으로 설정
        AtomicInteger progressRate = new AtomicInteger(0);

        Disposable feedbackProgressRateDisposable = Flux.interval(Duration.ofSeconds(1))
                .takeWhile(i -> progressRate.get() < 49) // progressRate가 49 미만일 때만 실행
                .doOnNext(tick -> {
                    // progressRate가 49까지만 증가
                    synchronized (progressRate) {
                        if (progressRate.get() >= 49) {
                            return;
                        }
                        feedbackEntity.setProgressRate(progressRate.incrementAndGet());
                        feedbackRepository.save(feedbackEntity);
                    }
                })
                .doOnComplete(() -> log.debug("Feedback progress rate increment completed"))
                .subscribe();
        
        final FeedbackEntity closureOldFeedbackEntity = oldFeedbackEntity;
        String jsonData = "{\"image_url\": " + "\"" + imageUrl + "\"}";
        webClientBuilder.build()
                .post()
                .uri(feedbackServerHost + "/request")
                .header("Content-Type", "application/json")
                .bodyValue(jsonData)
                .retrieve()
                .bodyToMono(FeedbackEntity.class)
                .doOnError(error -> {
                    log.error("Feedback request failed: {}", error.getMessage());

                    feedbackProgressRateDisposable.dispose();

                    usedCredit.refundCredit();
                    feedbackEntity.setState(FeedbackState.FAIL);
                    LocalDateTime NOW = LocalDateTime.now();
                    feedbackEntity.setCreatedAt(NOW);
                    creditRepository.save(usedCredit);
                    feedbackRepository.save(feedbackEntity);
                    memberRepository.save(me);
                })
                .doOnNext(pythonResponse -> {
                    log.debug(pythonResponse.toString());
                    try {
                        feedbackProgressRateDisposable.dispose();

                        if (FeedbackState.COMPLETED.equals(pythonResponse.getState())) {
                            synchronized (progressRate) {
                                progressRate.set(50);
                            }
                            feedbackEntity.setProgressRate(50);
                            feedbackRepository.save(feedbackEntity);

                            Disposable studyProgressRateDisposable = Flux.interval(Duration.ofSeconds(1))
                                    .takeWhile(i -> progressRate.get() < 99) // progressRate가 99 미만일 때만 실행
                                    .doOnNext(tick -> {
                                        // progressRate가 99까지만 증가
                                        synchronized (progressRate) {
                                            if (progressRate.get() >= 99) {
                                                return;
                                            }
                                            feedbackEntity.setProgressRate(progressRate.incrementAndGet());
                                            feedbackRepository.save(feedbackEntity);
                                        }
                                    })
                                    .doOnComplete(() -> log.debug("Study progress rate increment completed"))
                                    .subscribe();
    
                            for (FeedbackResultEntity fre : pythonResponse.getFeedbackResults()) {
                                fre.setFeedbackEntity(feedbackEntity);
                                if ("evaluation".equals(fre.getFeedbackType())) {
                                    feedbackResultsEvaluationLinkAdd(fre);
                                }
                            }

                            studyProgressRateDisposable.dispose();
                            synchronized (progressRate) {
                                progressRate.set(100);
                            }
                            feedbackEntity.setProgressRate(100);
                            feedbackRepository.save(feedbackEntity);
                        }

                        commonUtil.copyNonNullProperties(pythonResponse, feedbackEntity);
                        LocalDateTime NOW = LocalDateTime.now();
                        feedbackEntity.setCreatedAt(NOW);

                        CreditUsedHistoryEntity historyEntity = CreditUsedHistoryEntity.builder()
                                .type(usedCredit.getType())
                                .usedDate(NOW)
                                .memberEntity(me)
                                .feedbackEntity(feedbackEntity)
                                .build();
                        if (closureOldFeedbackEntity != null) {
                            closureOldFeedbackEntity.setIsHead(false);
                            feedbackRepository.save(closureOldFeedbackEntity);
                        }
                        creditUsedHistoryRepository.save(historyEntity);
                        feedbackRepository.save(feedbackEntity);
                    } catch (NullPointerException e) {
                        log.warn("null point exception 발생");
                    } catch (Exception e) {
                        log.warn(e.getCause().getMessage());
                        log.warn(e.getMessage());
                    }
                })
                .timeout(Duration.ofSeconds(200))
                .subscribe(
                    response -> log.debug("Response received: {}", response),
                    error -> log.debug("Error handled: {}", error.getMessage())
                );

        return new ResponseEntity<>(serialNumber, HttpStatusCode.valueOf(200));
    }

    public void feedbackResultsEvaluationLinkAdd(FeedbackResultEntity feedbackResultEntity) {
        @AllArgsConstructor
        @Getter
        class CategoryScore {
            public String key;
            public int score;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<FeedbackResultJSON> evaluationResults = objectMapper.readValue(feedbackResultEntity.getFeedbackContent(), new TypeReference<List<FeedbackResultJSON>>() {
            });
            for (FeedbackResultJSON evaluationClass : evaluationResults) {
                PriorityQueue<CategoryScore> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(CategoryScore::getScore));
                for (FeedbackResultJSON.Evaluation evaluation : evaluationClass.getEvaluations()) {
                    String name = evaluation.getName();
                    String namePart = name.contains("(") ? name.split("\\(")[0] + "~" : name;
                    priorityQueue.add(new CategoryScore(String.format("%s:%s", evaluationClass.getCategory(), namePart), evaluation.getScore()));
                }

                int MAX_STUDY_SIZE = 3;
                ArrayList<StudyInfoDTO> studies = new ArrayList<>(MAX_STUDY_SIZE);
                for (int i = 0; i < MAX_STUDY_SIZE; i++) studies.add(null);

                int ascendingEvaluationIndex = 0;
                while (!priorityQueue.isEmpty()) {
                    if (ascendingEvaluationIndex >= MAX_STUDY_SIZE) break;

                    int getSize = MAX_STUDY_SIZE - ascendingEvaluationIndex;
                    int pageSize = getSize;

                    CategoryScore categoryScore = priorityQueue.poll();
                    if (categoryScore.getScore() >= 90) break;

                    int countStudy = externalStudyContentsRepository.countStudies(categoryScore.getKey());
                    if (countStudy == 0) continue;

                    int randomPageNumber = ((int) (Math.random() * countStudy)) / pageSize;

                    int responseStudyIndex = ascendingEvaluationIndex;
                    for (StudyInfoDTO study : externalStudyContentsRepository.getStudies(categoryScore.getKey(), PageRequest.of(randomPageNumber, pageSize))) {
                        studies.set(responseStudyIndex, study);
                        responseStudyIndex++;
                        if(responseStudyIndex >= MAX_STUDY_SIZE) break;
                    }

                    ascendingEvaluationIndex++;
                }
                while (!studies.isEmpty() && studies.get(studies.size() - 1) == null) {
                    studies.remove(studies.size() - 1);
                }
                evaluationClass.setStudies(studies);
            }

            feedbackResultEntity.setFeedbackContent(objectMapper.writeValueAsString(evaluationResults));
        } catch (Exception e) {
            log.warn("python response가 양식이 이상함: {}", e.getMessage());
        }
    }

    boolean feedbackReview(String serialNumber,
                           boolean isLike,
                           String review,
                           CustomUserDetails userDetails) {
        if (review.length() < 9) {
            throw new BadFeedbackRequestException("too short review");
        }
        FeedbackEntity feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber).orElseThrow(() -> new BadFeedbackRequestException("Invalid SerialNumber"));

        if (!feedbackEntity.getMemberEntity().getUid().equals(userDetails.getUid())) {
            throw new NoPermissionException("Access Rejected");
        }
        feedbackEntity.setUserReview(isLike);
        feedbackEntity.setUserReviewDetail(review);
        feedbackRepository.save(feedbackEntity);
        return true;
    }


    public List<PastFeedbackDto> getFeedbackRecentOrder(long uid, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidAndStateNotOrderByCreatedAtDesc(uid, FeedbackState.FAIL, pageable);
        return convertFeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> getFeedbackCreatedAtOrder(long uid, int page) {
        LocalDateTime oneYearAgo = LocalDateTime.now().minus(1, ChronoUnit.YEARS);
        List<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidAndStateAndCreatedAtAfterOrderByCreatedAtAsc(uid, "COMPLETED", oneYearAgo);
        List<FeedbackEntity> rtFeedbackEntities = new ArrayList<>();
        int val=0;
        for(FeedbackEntity feedback:feedbackEntities){
            if(feedback.getTotalScore()>=val){
                rtFeedbackEntities.add(feedback);
                val=feedback.getTotalScore();
            }
        }
        return convertFeedbackEntityToDto(rtFeedbackEntities);
    }

    public List<PastFeedbackDto> getFeedbackTotalScoreOrder(long uid, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidAndStateNotOrderByTotalScoreDesc(uid, FeedbackState.FAIL, pageable);
        return convertFeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> getFeedbackBookmark(long uid, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidAndIsBookmarkedOrderByCreatedAtDesc(uid, true, pageable);

        return convertFeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> convertFeedbackEntityToDto(Slice<FeedbackEntity> feedbackEntities) {
        List<PastFeedbackDto> feedbackDtos = new ArrayList<>();
        for (FeedbackEntity f : feedbackEntities) {
            feedbackDtos.add(convertFeedbackEntityToDto(f));
        }
        return feedbackDtos;
    }

    public List<PastFeedbackDto> convertFeedbackEntityToDto(List<FeedbackEntity> feedbackEntities) {
        List<PastFeedbackDto> feedbackDtos = new ArrayList<>();
        for (FeedbackEntity f : feedbackEntities) {
            feedbackDtos.add(convertFeedbackEntityToDto(f));
        }
        return feedbackDtos;
    }

    public void turnBookmark(String serialNumber, long uid, boolean target) {
        FeedbackEntity feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber).orElseThrow(() -> new CannotFindBySerialNumberException("feedback not exists"));
        if (feedbackEntity.getMemberEntity().getUid() != uid) {
            throw new NoPermissionException("You are not allowed to turn this feedback");
        }
        feedbackEntity.setIsBookmarked(target);
        feedbackRepository.save(feedbackEntity);
    }

    public PastFeedbackDto convertFeedbackEntityToDto(FeedbackEntity feedbackEntity) {
        return PastFeedbackDto
                .builder()
                .isBookmarked(feedbackEntity.getIsBookmarked())
                .pictureUrl(feedbackEntity.getPictureUrl())
                .createdAt(feedbackEntity.getCreatedAt())
                .version(feedbackEntity.getVersion())
                .serialNumber(feedbackEntity.getSerialNumber())
                .totalScore(feedbackEntity.getTotalScore())
                .isSelected(false)
                .feedbackResults(feedbackEntity.getFeedbackResults())
                .state(feedbackEntity.getState())
                .build();
    }

    public List<CommentDto> findCommentBySerialNumber(String serialNumber) {
        return commentRepository.findByFeedbackSerialNumberOrderByCreatedAtDesc(serialNumber);
    }

    @Transactional
    public void addComment(String serialNumber, CustomUserDetails userDetails, String body) {
        MemberEntity me = memberRepository.findBySerialNumber(userDetails.getSerialNumber());
        FeedbackEntity feedback = feedbackRepository.findBySerialNumber(serialNumber).orElseThrow(() -> new IllegalArgumentException("Invalid Feedback"));
        FeedbackCommentEntity comment = FeedbackCommentEntity
                .builder()
                .contents(body)
                .feedback(feedback)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .memberEntity(me)
                .build();
        try {
            commentRepository.save(comment);
        } catch (EntityNotFoundException e) {
            throw new UserNotFoundException("User Not Found");
        }

    }
}
