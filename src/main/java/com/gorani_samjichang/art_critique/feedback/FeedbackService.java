package com.gorani_samjichang.art_critique.feedback;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final int PAGESIZE = 4;
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

    public List<PastFeedbackDto> getFeedbackRecentOrder(String email, int page) {
        Pageable pageable = PageRequest.of(page, PAGESIZE);
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityEmailOrderByCreatedAtDesc(email, pageable);
        return FeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> getFeedbackTotalScoreOrder(String email, int page) {
        Pageable pageable = PageRequest.of(page, PAGESIZE);
        Slice<FeedbackEntity> feedbackEntities = feedbackRepository.findByMemberEntityEmailOrderByTotalScoreDesc(email, pageable);
        return FeedbackEntityToDto(feedbackEntities);
    }

    public List<PastFeedbackDto> FeedbackEntityToDto(Slice<FeedbackEntity> feedbackEntities) {
        List<PastFeedbackDto> feedbackDtos = new ArrayList<>();
        for (FeedbackEntity f : feedbackEntities) {
            feedbackDtos.add(FeedbackEntityToDto(f));
        }
        return feedbackDtos;

    }

    public PastFeedbackDto FeedbackEntityToDto(FeedbackEntity feedbackEntity) {
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
