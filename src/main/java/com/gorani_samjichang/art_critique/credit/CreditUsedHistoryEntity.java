package com.gorani_samjichang.art_critique.credit;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.gorani_samjichang.art_critique.feedback.FeedbackController;
import com.gorani_samjichang.art_critique.feedback.FeedbackEntity;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@ToString
@Entity
@Table(name="credit_used_history")
public class CreditUsedHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chid;
    private LocalDateTime usedDate;
    private String type;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid")
    @JsonBackReference
    private MemberEntity memberEntity;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fid")
    private FeedbackEntity feedbackEntity;
}
