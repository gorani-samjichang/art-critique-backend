package com.gorani_samjichang.art_critique.feedback;

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
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

    @Scheduled(fixedRate = 1000 * 60 * 15)
    public void checkGoodImages() {
        goodImages = feedbackRepository.findGoodImage();
    }

    List<FeedbackUrlDto> getGoodImage() {
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

        String jsonData = "{\"image_url\": " + "\"" + imageUrl + "\"}";
        webClientBuilder.build()
                .post()
                .uri(feedbackServerHost + "/request")
                .header("Content-Type", "application/json")
                .bodyValue(jsonData)
                .retrieve()
                .bodyToMono(FeedbackEntity.class)
                .doOnError(error -> {
                    usedCredit.refundCredit();
                    feedbackEntity.setState(FeedbackState.FAIL);
                    LocalDateTime NOW = LocalDateTime.now();
                    feedbackEntity.setCreatedAt(NOW);
                    creditRepository.save(usedCredit);
                    feedbackRepository.save(feedbackEntity);
                    memberRepository.save(me);
                })
                .doOnNext(pythonResponse -> {
                    System.out.println(pythonResponse.toString());
                    try {
                        for (FeedbackResultEntity fre : pythonResponse.getFeedbackResults()) {
                            fre.setFeedbackEntity(feedbackEntity);
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
                        System.out.println("null point exception 발생");
                    } catch (Exception e) {
                        System.out.println(e.getCause());
                        System.out.println(e.getMessage());
                    }
                })
                .subscribe();

        return new ResponseEntity<>(serialNumber, HttpStatusCode.valueOf(200));
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
        System.out.println(oneYearAgo);
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
