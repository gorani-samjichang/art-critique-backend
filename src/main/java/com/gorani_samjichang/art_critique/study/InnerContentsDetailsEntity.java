package com.gorani_samjichang.art_critique.study;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "innerContentsDetails", indexes = {@Index(name = "inner_contents_detail_idx", columnList = "contents_id, cid")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerContentsDetailsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cid;

    @ManyToOne
    @JoinColumn(name = "contents_id")
    private InnerContentsEntity innerContents;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String type;

}
