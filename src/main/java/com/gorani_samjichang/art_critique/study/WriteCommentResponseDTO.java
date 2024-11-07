package com.gorani_samjichang.art_critique.study;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class WriteCommentResponseDTO {
    private String content;
    private LocalDateTime createdAt;
    private boolean like;
    private String memberName;
    private String memberProfile;
}
