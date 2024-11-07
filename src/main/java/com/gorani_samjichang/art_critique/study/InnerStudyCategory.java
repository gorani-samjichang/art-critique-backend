package com.gorani_samjichang.art_critique.study;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "InnerStudyCategory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerStudyCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categroyNum;
    private String categoryName;

    @ManyToOne
    @JoinColumn(name = "fid")
    private InnerStudyField field;

}
