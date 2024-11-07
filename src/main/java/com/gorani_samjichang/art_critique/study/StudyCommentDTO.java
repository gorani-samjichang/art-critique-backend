package com.gorani_samjichang.art_critique.study;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StudyCommentDTO {
    private String memberName;
    private String memberProfile;
    private boolean isLike;
    private LocalDateTime createdAt;
    private String content;
}
