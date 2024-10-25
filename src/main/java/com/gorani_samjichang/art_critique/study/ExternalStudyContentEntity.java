package com.gorani_samjichang.art_critique.study;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name="externalStudyContents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalStudyContentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cid;
    private String url;
    private String title;
    private String type;
    private Long stamp;
    private String author;
}
