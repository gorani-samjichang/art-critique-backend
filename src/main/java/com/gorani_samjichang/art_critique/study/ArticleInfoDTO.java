package com.gorani_samjichang.art_critique.study;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class ArticleInfoDTO {
    private Long articleCount;
    private Long totalView;
    private Long totalLike;
    private List<String> tags;

    public ArticleInfoDTO(Long articleCount, Long totalView, Long totalLike) {
        this.articleCount = articleCount == null ? 0 : articleCount;
        this.totalView = totalView == null ? 0 : totalView;
        this.totalLike = totalLike == null ? 0 : totalLike;

    }
}
