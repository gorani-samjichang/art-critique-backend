package com.gorani_samjichang.art_critique.credit;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class CreditDto {
    private LocalDateTime purchaseDate;
    private LocalDateTime expireDate;
    private Integer purchaseAmount;
    private Integer remainAmount;
    private Integer usedAmount;
    private String type; //PREPAYMENT, SUBSCRIBE
    private String state;
}
