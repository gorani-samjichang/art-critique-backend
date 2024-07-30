package com.gorani_samjichang.art_critique.feedback;

import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.common.exceptions.BadFeedbackRequest;
import com.gorani_samjichang.art_critique.common.exceptions.NoPermissionException;
import com.gorani_samjichang.art_critique.common.exceptions.ServiceNotAvailableException;
import com.gorani_samjichang.art_critique.credit.CreditEntity;
import com.gorani_samjichang.art_critique.credit.CreditRepository;
import com.gorani_samjichang.art_critique.credit.CreditUsedHistoryEntity;
import com.gorani_samjichang.art_critique.credit.CreditUsedHistoryRepository;
import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final int PAGESIZE = 4;
    final MemberRepository memberRepository;
    final CreditRepository creditRepository;
    final CreditUsedHistoryRepository creditUsedHistoryRepository;
    final CommonUtil commonUtil;
    final BCryptPasswordEncoder bCryptPasswordEncoder;
    final WebClient.Builder webClientBuilder;
    @Value("${feedback.server.host}")
    private String feedbackServerHost;
    final EmitterService emitterService;


    String[] dummyTodayGoodImage = new String[]{
            "https://picsum.photos/id/88/180/160",
            "https://picsum.photos/id/51/200/200",
            "https://picsum.photos/id/50/260/240",
            "https://picsum.photos/id/9/180/160",
            "https://picsum.photos/id/55/260/280",
            "https://picsum.photos/id/70/220/220",
            "https://picsum.photos/id/57/160/300",
            "https://picsum.photos/id/19/300/120",
            "https://picsum.photos/id/99/100/100",
            "https://picsum.photos/id/26/300/260",
            "https://picsum.photos/id/71/120/120",
            "https://picsum.photos/id/69/160/160",
            "https://picsum.photos/id/39/100/120",
            "https://picsum.photos/id/27/240/160",
            "https://picsum.photos/id/15/240/140"
    };

    String[] getGoodImage() {
        String[] todayGoodImage = dummyTodayGoodImage;
        return todayGoodImage;
    }

    public String requestFeedback(
            MultipartFile imageFile,
            CustomUserDetails userDetails) throws IOException {
        MemberEntity me = userDetails.getMemberEntity();
        CreditEntity usedCredit = creditRepository.usedCreditEntityByRequest(userDetails.getUid());
        if (me.getCredit() <= 0 || usedCredit == null) {
            return "no Credit";
        }

        String serialNumber = UUID.randomUUID().toString();
        String imageUrl = commonUtil.uploadToStorage(imageFile, serialNumber);
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

        feedbackRepository.save(feedbackEntity);

        me.addFeedback(feedbackEntity);
        me.setCredit(me.getCredit() - 1);
        usedCredit.useCredit();
        creditRepository.save(usedCredit);

        memberRepository.save(me);

        try {
            String jsonData = "{\"name\": " + "\"" + "imageUrl" + "\"}";
            webClientBuilder.build()
                    .post()
                    .uri(feedbackServerHost + "/request")
                    .header("Content-Type", "application/json")
                    .bodyValue(jsonData)
                    .retrieve()
                    .bodyToMono(FeedbackEntity.class)
                    .doOnNext(pythonResponse -> {
                        if (pythonResponse != null) {
                            commonUtil.copyNonNullProperties(pythonResponse, feedbackEntity);
                            for (FeedbackResultEntity fre : feedbackEntity.getFeedbackResults()) {
                                fre.setFeedbackEntity(feedbackEntity);
                            }
                        }

                        LocalDateTime NOW = LocalDateTime.now();
                        feedbackEntity.setCreatedAt(NOW);
                        CreditUsedHistoryEntity historyEntity = CreditUsedHistoryEntity.builder()
                                .type(usedCredit.getType())
                                .usedDate(NOW)
                                .feedbackEntity(feedbackEntity)
                                .build();
                        me.addCreditHistory(historyEntity);
                        creditUsedHistoryRepository.save(historyEntity);
                        feedbackRepository.save(feedbackEntity);
                    })
                    .subscribe();

        } catch (Exception e) {
            throw new ServiceNotAvailableException("Feedback Server Error");
        }

        return serialNumber;
    }

    boolean feedbackReview(String serialNumber,
                           boolean isLike,
                           String review,
                           CustomUserDetails userDetails) {
        if (review.length() < 9) {
            throw new BadFeedbackRequest("too short review");
        }
        FeedbackEntity feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber).orElseThrow(() -> new BadFeedbackRequest("Invalid SerialNumber"));

        if (!feedbackEntity.getMemberEntity().getUid().equals(userDetails.getUid())) {
            throw new NoPermissionException("Access Rejected");
        }
        feedbackEntity.setUserReview(isLike);
        feedbackEntity.setUserReviewDetail(review);
        feedbackRepository.save(feedbackEntity);
        return true;
    }


    public List<PastFeedbackDto> getFeedbackRecentOrder(long uid, int page) {
        Pageable pageable = PageRequest.of(page, PAGESIZE);
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidOrderByCreatedAtDesc(uid, pageable);
        return convertFeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> getFeedbackCreatedAtOrder(long uid, int page) {
        Pageable pageable = PageRequest.of(page, PAGESIZE);
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidOrderByCreatedAtAsc(uid, pageable);
        return convertFeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> getFeedbackTotalScoreOrder(long uid, int page) {
        Pageable pageable = PageRequest.of(page, PAGESIZE);
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityUidOrderByTotalScoreDesc(uid, pageable);
        return convertFeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> getFeedbackBookmark(long uid, int page){
        Pageable pageable = PageRequest.of(page, PAGESIZE);
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
                .build();
    }
}