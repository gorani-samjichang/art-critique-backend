package com.gorani_samjichang.art_critique.member;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class JwtInfoVo {
    Long uid;
    String email;
    String serialNumber;
    String role;
}
