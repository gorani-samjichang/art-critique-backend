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
            CustomUserDetails userDetails) throws IOException, UserNotFoundException {
        MemberEntity me = memberRepository.findById(userDetails.getUid()).orElseThrow(() -> new UserNotFoundException("user not found"));
        CreditEntity usedCredit = creditRepository.usedCreditEntityByRequest(userDetails.getUid());
        if (me.getCredit() <= 0 || usedCredit == null) {
            return new ResponseEntity<>("noCredit", HttpStatusCode.valueOf(200));
        }

        String serialNumber = UUID.randomUUID().toString();
        String pictureUUID = UUID.randomUUID().toString();
        String imageUrl = commonUtil.uploadToStorage(imageFile, pictureUUID);
        FeedbackEntity feedbackEntity = FeedbackEntity
                .builder()
                .serialNumber(serialNumber)
                .state("NOT_STARTED")
                .progressRate(0)
                .isBookmarked(false)
                .isPublic(true)
                .pictureUrl(imageUrl)
                .isHead(true)
                .tail(null)
                .build();

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
                    feedbackEntity.setProgressRate(progressRate.incrementAndGet());
                    feedbackRepository.save(feedbackEntity);
                })
                .doOnComplete(() -> log.debug("Feedback progress rate increment completed"))
                .subscribe();

        String jsonData = "{\"image_url\": " + "\"" + imageUrl + "\"}";
        webClientBuilder.build()
                .post()
                .uri(feedbackServerHost + "/request")
                .header("Content-Type", "application/json")
                .bodyValue(jsonData)
                .retrieve()
                .bodyToMono(FeedbackEntity.class)
                .doOnError(error -> {
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
                            feedbackEntity.setProgressRate(50);
                            feedbackRepository.save(feedbackEntity);

                            progressRate.set(50);
                            Disposable studyProgressRateDisposable = Flux.interval(Duration.ofSeconds(1))
                                    .takeWhile(i -> progressRate.get() < 99) // progressRate가 99 미만일 때만 실행
                                    .doOnNext(tick -> {
                                        // progressRate가 99까지만 증가
                                        feedbackEntity.setProgressRate(progressRate.incrementAndGet());
                                        feedbackRepository.save(feedbackEntity);
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
                .subscribe();

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
            for (FeedbackResultJSON data : evaluationResults) {
                PriorityQueue<CategoryScore> pq = new PriorityQueue<>(Comparator.comparingInt(CategoryScore::getScore));
                for (FeedbackResultJSON.Evaluation evaluation : data.getEvaluations()) {
                    String name = evaluation.getName();
                    String namePart = name.contains("(") ? name.split("\\(")[0] + "~" : name;
                    pq.add(new CategoryScore(String.format("%s:%s", data.getCategory(), namePart), evaluation.getScore()));
                }
                List<StudyInfoDTO> studies = new ArrayList<>(3);
                studies.add(null);studies.add(null);studies.add(null);
                int idx = 0;
                while (!pq.isEmpty()) {
                    CategoryScore cs = pq.poll();
                    if (cs.getScore() >= 90) break;
                    int count = externalStudyContentsRepository.countStudies(cs.getKey());
                    if (count == 0) continue;
                    int rand = (int) (Math.random() * count) / (3 - idx);
                    int j = 0;
                    for (StudyInfoDTO study : externalStudyContentsRepository.getStudies(cs.getKey(), PageRequest.of(rand, 3 - idx))) {
                        if(j+idx == 3) break;
                        studies.set(idx + j, study);
                        j++;
                    }
                    idx++;
                    if (idx == 3) break;
                }
                while (!studies.isEmpty() && studies.get(studies.size() - 1) == null) {
                    studies.remove(studies.size() - 1);
                }
                data.setStudies(studies);
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
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidAndIsHeadAndStateNotOrderByCreatedAtDesc(uid, true, FeedbackState.FAIL, pageable);
        return convertFeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> getFeedbackCreatedAtOrder(long uid, int page) {
        LocalDateTime oneYearAgo = LocalDateTime.now().minus(1, ChronoUnit.YEARS);
        List<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidAndIsHeadAndStateAndCreatedAtAfterOrderByCreatedAtAsc(uid, true, "COMPLETED", oneYearAgo);
        return convertFeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> getFeedbackTotalScoreOrder(long uid, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidAndIsHeadAndStateNotOrderByTotalScoreDesc(uid, true, FeedbackState.FAIL, pageable);
        return convertFeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> getFeedbackBookmark(long uid, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidAndIsBookmarkedAndIsHeadOrderByCreatedAtDesc(uid, true, true, pageable);

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
