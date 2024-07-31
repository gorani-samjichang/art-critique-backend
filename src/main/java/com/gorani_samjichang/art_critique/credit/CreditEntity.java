package com.gorani_samjichang.art_critique.credit;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.gorani_samjichang.art_critique.appConstant.CreditState;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@ToString
@Entity
@Table(name="credit")
public class CreditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cid;
    private LocalDateTime purchaseDate;
    private LocalDateTime expireDate;
    private Integer purchaseAmount;
    private Integer remainAmount;
    private Integer usedAmount;
    private String type; //PREPAYMENT, SUBSCRIBE
    private String state; //VALID, EXPIRED, USED_UP
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid")
    @JsonBackReference
    private MemberEntity memberEntity;

    public void useCredit() {
        this.remainAmount--;
        this.usedAmount++;
        this.memberEntity.setCredit(this.memberEntity.getCredit() - 1);
        if (remainAmount <= 0) this.state = "USED_UP";
    }
    public void refundCredit() {
        this.remainAmount++;
        this.usedAmount--;
        this.memberEntity.setCredit(this.memberEntity.getCredit() + 1);
        this.state = CreditState.VALID;
    }
}
