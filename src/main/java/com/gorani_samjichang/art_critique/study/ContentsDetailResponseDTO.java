package com.gorani_samjichang.art_critique.study;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ContentsDetailResponseDTO {
    private String articleTitle;
    private ArticleMetaDataDTO articleMetaData;
    private List<ArticleContentsDTO> articleContent;

    public ContentsDetailResponseDTO(String title, String author, String authorProfile, String authorSerialNumber, String categoryName, Long bigCategoryNumber, Long smallCategoryNumber, Long like, Long view, LocalDateTime date) {
        articleTitle = title;
        articleMetaData = ArticleMetaDataDTO.builder()
                .like(like)
                .date(date)
                .view(view)
                .author(author)
                .authorProfile(authorProfile)
                .authorSerialNumber(authorSerialNumber)
                .categoryName(categoryName)
                .BigCategoryNumber(bigCategoryNumber)
                .SmallCategoryNumber(smallCategoryNumber)
                .build();
    }
}
