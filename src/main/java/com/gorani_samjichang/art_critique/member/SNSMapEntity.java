package com.gorani_samjichang.art_critique.member;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="sns_map")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SNSMapEntity {
    @Id
    private String key;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", referencedColumnName = "uid")
    private MemberEntity member;
}
