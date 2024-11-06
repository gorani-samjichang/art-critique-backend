package com.gorani_samjichang.art_critique.study;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ArticleMetaDataDTO {
    private String categoryName;
    private Long BigCategoryNumber;
    private Long SmallCategoryNumber;
    private String author;
    private String authorSerialNumber;
    private String authorProfile;
    private LocalDateTime date;
    private Long view;
    private Long like;
    private List<String> tags;
}
