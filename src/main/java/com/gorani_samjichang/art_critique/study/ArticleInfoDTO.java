package com.gorani_samjichang.art_critique.study;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ArticleInfoDTO {
    private long articleCount;
    private long totalView;
    private long totalLike;
    private List<String> tags;
}
