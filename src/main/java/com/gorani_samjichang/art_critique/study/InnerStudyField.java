package com.gorani_samjichang.art_critique.study;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "InnerStudyField")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerStudyField {
    @Id
    private Long fid;
    private String title;
}
