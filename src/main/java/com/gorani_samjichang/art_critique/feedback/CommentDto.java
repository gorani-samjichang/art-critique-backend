package com.gorani_samjichang.art_critique.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class CommentDto {
    private String authorName;
    private String authorSerialNumber;
    private String authorProfile;
    private LocalDateTime date;
    private String contents;
}
