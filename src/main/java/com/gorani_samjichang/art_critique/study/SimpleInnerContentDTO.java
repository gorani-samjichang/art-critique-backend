package com.gorani_samjichang.art_critique.study;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class SimpleInnerContentDTO {
    private String articleTitle;
    private String articleSerialNumber;
}
