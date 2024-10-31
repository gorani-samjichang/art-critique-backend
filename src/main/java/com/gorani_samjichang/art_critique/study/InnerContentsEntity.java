package com.gorani_samjichang.art_critique.study;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "innerContents", indexes = @Index(name = "inner_contents_serialnumber_idx", columnList = "serialNumber", unique = true))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerContentsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cid;
    private String serialNumber;
    private String content;
    private String thumbnailUrl;
    private String level;
    private String author;
    private LocalDateTime createdAt;
    private Long view;
    private Long likes;
    private String title;

    @ElementCollection
    @CollectionTable(name = "inner_contents_hashtags", joinColumns = @JoinColumn(name = "cid"), indexes = @Index(name = "idx_hashtag", columnList = "tags"))
    private List<String> tags;

    @ManyToMany
    @JoinTable(
            name = "inner_contents_classification",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<InnerStudyCategory> subCategories = new ArrayList<>();
}
