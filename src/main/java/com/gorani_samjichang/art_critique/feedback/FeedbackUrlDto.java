package com.gorani_samjichang.art_critique.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class FeedbackUrlDto {
    private String pictureUrl;
    private String serialNumber;
}
