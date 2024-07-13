package com.gorani_samjichang.art_critique.feedback;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="feedback")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fid;
    private String picture_url;
    private Integer version;
    private Long used_token;
    private LocalDateTime created_at;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid")
    @JsonBackReference
    private MemberEntity memberEntity;
    private Boolean user_review;
    @Column(columnDefinition = "TEXT")
    private String user_review_detail;
    private Boolean is_public;
    @OneToMany(mappedBy = "feedbackEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private List<FeedbackResultEntity> feedback_results;

    public void addFeedbackResult(FeedbackResultEntity feedbackResultEntity) {
        if (feedback_results == null) feedback_results = new ArrayList<>();
        this.feedback_results.add(feedbackResultEntity);
        feedbackResultEntity.setFeedbackEntity(this);
    }

    public void removeFeedbackResult(FeedbackResultEntity feedbackResultEntity) {
        this.feedback_results.remove(feedbackResultEntity);
        feedbackResultEntity.setFeedbackEntity(null);
    }
}
