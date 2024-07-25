package com.gorani_samjichang.art_critique.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public boolean emailCheck(String email) {
        return memberRepository.existsByEmail(email);
    }

    public boolean nicknameCheck(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    public MemberDto readMember(String email) {
        MemberEntity member = memberRepository.findByEmail(email);
        return MemberEntityToDto(member);
    }

    public int readCredit(String email) {
        MemberEntity member = memberRepository.findByEmail(email);
        return member.getCredit();
    }

    public MemberDto MemberEntityToDto(MemberEntity memberEntity) {
        return MemberDto.builder()
                .email(memberEntity.getEmail())
                .profile(memberEntity.getProfile())
                .role(memberEntity.getRole())
                .level(memberEntity.getLevel())
                .nickname(memberEntity.getNickname())
                .build();
    }
}
