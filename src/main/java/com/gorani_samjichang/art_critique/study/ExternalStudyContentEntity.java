package com.gorani_samjichang.art_critique.study;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="externalStudyContents", indexes = @Index(name = "idx_studycontents_about", columnList = "about"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalStudyContentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cid;
    private String title;
    private String url;
    private Long stamp;
    private String about;
    private String keyword;
    private String author;
}
