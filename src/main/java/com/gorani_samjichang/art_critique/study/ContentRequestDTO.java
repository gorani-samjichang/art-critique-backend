package com.gorani_samjichang.art_critique.study;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ContentRequestDTO {
    private Long bigCategory;
    private Long smallCategory;
    private String level;
    private String articleTitle;
    private List<ArticleContent> articleContent;
    private List<String> tagList;

}
