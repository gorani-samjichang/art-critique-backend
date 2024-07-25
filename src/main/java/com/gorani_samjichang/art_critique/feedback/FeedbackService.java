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
