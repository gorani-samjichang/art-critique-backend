package com.gorani_samjichang.art_critique.study;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "InnerStudyField")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerStudyField {
    @Id
    private Long categoryNumber;
    private String categoryTitle;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "field")
    private List<InnerStudyCategory> detail;
}
