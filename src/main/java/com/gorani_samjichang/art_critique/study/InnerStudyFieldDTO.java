package com.gorani_samjichang.art_critique.study;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class InnerStudyFieldDTO {
    private String categoryTitle;
    private Long categoryNumber;
    private List<InnerStudyCategoryDTO> detail;

    public InnerStudyFieldDTO(InnerStudyField innerStudyField) {
        this.categoryTitle = innerStudyField.getCategoryTitle();
        this.categoryNumber = innerStudyField.getCategoryNumber();
        this.detail = innerStudyField.getDetail().stream().map(InnerStudyCategoryDTO::new).toList();
    }
}
