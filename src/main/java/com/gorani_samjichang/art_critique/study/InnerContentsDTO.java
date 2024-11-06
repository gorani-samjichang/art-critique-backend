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
public class InnerContentsDTO {
    private String articleTitle;
    private String articleThumbnail;
    private String articleAuthor;
    private String articleSerialNumber;
    private String articleCategoryName;
    private String articleBigCategoryName;
    private LocalDateTime articleDate;
    private Long like;
    private Long view;

}
