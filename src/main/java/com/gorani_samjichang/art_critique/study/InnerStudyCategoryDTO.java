package com.gorani_samjichang.art_critique.study;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class InnerStudyCategoryDTO {
    private String categoryName;
    private Long categroyNum;

    public InnerStudyCategoryDTO(InnerStudyCategory innerStudyCategory) {
        this.categoryName = innerStudyCategory.getCategoryName();
        this.categroyNum = innerStudyCategory.getCategroyNum();
    }
}
