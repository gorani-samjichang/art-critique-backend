package com.gorani_samjichang.art_critique.study;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class InnerContentsCategoryDTO {
    private String author;
    private String authorProfile;
    private LocalDateTime date;
    private String serialNumber;
    private String thumbnail;
    private Long like;
    private String level;
    private String title;
    private Long view;
}
