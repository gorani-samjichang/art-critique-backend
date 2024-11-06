package com.gorani_samjichang.art_critique.study;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryNumber;
    private String categoryTitle;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "field")
    private List<InnerStudyCategory> detail;

    public void addDetail(InnerStudyCategory f) {
        if (this.detail == null) this.detail = new ArrayList<>();
        this.detail.add(f);
    }
}
