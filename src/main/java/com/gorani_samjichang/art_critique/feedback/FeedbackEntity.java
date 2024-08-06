package com.gorani_samjichang.art_critique.feedback;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@ToString
public class FeedbackEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fid;
    private String serialNumber;
    private String pictureUrl;
    private Integer totalScore;
    private Integer version;
    private Long inputTokens;
    private Long outputTokens;
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid")
    @JsonBackReference
    private MemberEntity memberEntity;
    private Boolean userReview;
    @Column(columnDefinition = "TEXT")
    private String userReviewDetail;
    private Boolean isPublic;
    private Boolean isBookmarked;
    private String tail;
    private String state; // NOT_STARTED, PENDING, COMPLETED
    private Integer progressRate;
    private Boolean isHead;
    @OneToMany(mappedBy = "feedbackEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = false)
    @JsonManagedReference
    private List<FeedbackResultEntity> feedbackResults;

    public void addFeedbackResult(FeedbackResultEntity feedbackResultEntity) {
        if (feedbackResults == null) feedbackResults = new ArrayList<>();
        this.feedbackResults.add(feedbackResultEntity);
        feedbackResultEntity.setFeedbackEntity(this);
    }

    public void removeFeedbackResult(FeedbackResultEntity feedbackResultEntity) {
        this.feedbackResults.remove(feedbackResultEntity);
        feedbackResultEntity.setFeedbackEntity(null);
    }
}
