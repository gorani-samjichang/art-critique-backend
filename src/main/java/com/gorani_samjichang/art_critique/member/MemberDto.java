package com.gorani_samjichang.art_critique.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class MemberDto {
    private String email;
    private String profile;
    private String role;
    private String level;
    private String nickname;
    private String serialNumber;
}
