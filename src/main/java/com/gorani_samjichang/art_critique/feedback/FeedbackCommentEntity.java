package com.gorani_samjichang.art_critique.feedback;

import com.gorani_samjichang.art_critique.member.MemberEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="comment", indexes = @Index(name="idx_feedback_comment_id", columnList = "serialNumber"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FeedbackCommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cid;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="serialNumber", referencedColumnName = "serialNumber")
    private FeedbackEntity feedback;
    private String contents;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="uid")
    private MemberEntity memberEntity;

}
