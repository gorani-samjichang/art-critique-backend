package com.gorani_samjichang.art_critique.study;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "externalStudyContentClassification",
        indexes = {@Index(name = "idx_category_keyword_contentId", columnList = "category, keyword, content_id", unique = true)}
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalStudyContentClassificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cid;
    private String category;
    private String keyword;
    @ManyToOne
    @JoinColumn(name = "content_id")
    private ExternalStudyContentEntity content;
}
