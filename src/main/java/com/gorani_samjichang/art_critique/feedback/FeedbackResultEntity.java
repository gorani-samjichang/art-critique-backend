package com.gorani_samjichang.art_critique.feedback;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "feedback_result")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long frid;
    private String feedbackType;
    private String feedbackDisplay;
    private String feedbackContent;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fid")
    @JsonBackReference
    private FeedbackEntity feedbackEntity;
}
