package com.gorani_samjichang.art_critique.study;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ContentRequestDTO {
    private Long bigCategory;
    private Long smallCategory;
    private String level;
    private String articleTitle;
    private List<ArticleContent> articleContent;
    private List<String> tagList;

}
