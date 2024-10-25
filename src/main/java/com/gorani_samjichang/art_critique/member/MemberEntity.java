package com.gorani_samjichang.art_critique.member;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.gorani_samjichang.art_critique.credit.CreditEntity;
import com.gorani_samjichang.art_critique.credit.CreditUsedHistoryEntity;
import com.gorani_samjichang.art_critique.feedback.FeedbackEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="member", indexes = @Index(name = "idx_member_serialNumber", unique = true, columnList = "serialNumber"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long uid;
    private String serialNumber;
    private String email;
    private String password;
    private Boolean isDeleted;
    private String nickname;
    private Integer credit;
    @Column(columnDefinition = "TEXT")
    private String profile;
    private String level;
    private Boolean open;
    private LocalDateTime createdAt;
    private String role; // ROLE_USER, ROLE_ADMIN 이정도만 생각하는중 ROLE_을 접두사로 쓰는건 Spring Security 의 정책
    @OneToMany(mappedBy = "memberEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = false)
    @JsonManagedReference
    @Builder.Default
    private List<FeedbackEntity> feedbacks = new ArrayList<>();
    @OneToMany(mappedBy = "memberEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = false)
    @JsonManagedReference
    @Builder.Default
    private List<CreditEntity> credits = new ArrayList<>();
    @OneToMany(mappedBy = "memberEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = false)
    @JsonManagedReference
    @Builder.Default
    private List<CreditUsedHistoryEntity> creditHistorys = new ArrayList<>();

    public void addFeedback(FeedbackEntity feedbackEntity) {
        feedbacks.add(feedbackEntity);
        feedbackEntity.setMemberEntity(this);
    }
    public void removeFeedback(FeedbackEntity feedbackEntity) {
        feedbacks.remove(feedbackEntity);
        feedbackEntity.setMemberEntity(null);
    }

    public void addCredit(CreditEntity creditEntity) {
        if (credits == null) credits = new ArrayList<>();
        credits.add(creditEntity);
        creditEntity.setMemberEntity(this);
    }
    public void removeCredit(CreditEntity creditEntity) {
        credits.remove(creditEntity);
        creditEntity.setMemberEntity(null);
    }

    public void addCreditHistory(CreditUsedHistoryEntity creditUsedHistoryEntity) {
        creditHistorys.add(creditUsedHistoryEntity);
        creditUsedHistoryEntity.setMemberEntity(this);
    }
    public void removeCreditHistory(CreditUsedHistoryEntity creditUsedHistoryEntity) {
        creditHistorys.remove(creditUsedHistoryEntity);
        creditUsedHistoryEntity.setMemberEntity(null);
    }
}
