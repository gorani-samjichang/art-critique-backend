package com.gorani_samjichang.art_critique.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class RetrieveFeedbackDto {
    private String serialNumber;
    private String pictureUrl;
    private Integer version;
    private LocalDateTime createdAt;
    private Boolean userReview;
    private String userReviewDetail;
    private Boolean isBookmarked;
    private List<FeedbackResultDto> feedbackResults;
    private String state;
    private String tail;
}
