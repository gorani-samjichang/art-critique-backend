package com.gorani_samjichang.art_critique.study;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private LocalDateTime deletedAt;

    public ContentsDetailResponseDTO(String title, String author, String authorProfile, String authorSerialNumber, String categoryName, Long bigCategoryNumber, Long smallCategoryNumber, Long like, Long view, LocalDateTime date, LocalDateTime deletedAt) {
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
