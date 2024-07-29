package com.gorani_samjichang.art_critique.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class PlanVo {
    Integer amount;
    Integer totalCost;
    Integer costPerImage;
}
