package com.gorani_samjichang.art_critique.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class PastFeedbackDto {
    private String serialNumber;
    private String pictureUrl;
    private Integer version;
    private LocalDateTime createdAt;
    private Boolean isBookmarked;
    private Boolean isSelected;
}