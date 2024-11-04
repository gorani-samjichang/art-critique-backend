package com.gorani_samjichang.art_critique.study;

import com.gorani_samjichang.art_critique.member.MemberEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;

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

    @ManyToOne
    private MemberEntity author;

    private LocalDateTime createdAt;
    private Long view;
    private Long likes;
    private String title;

    @ElementCollection
    @CollectionTable(name = "inner_contents_hashtags", joinColumns = @JoinColumn(name = "cid"), indexes = @Index(name = "idx_hashtag", columnList = "tags"))
    private HashSet<String> tags;

    @ManyToOne
    private InnerStudyCategory subCategory;
}
