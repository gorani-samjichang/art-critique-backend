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
    private Long categroyNum;
    String categoryName;

    @ManyToOne
    @JoinColumn(name = "fid")
    private InnerStudyField field;

}
