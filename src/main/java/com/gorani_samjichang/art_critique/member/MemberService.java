package com.gorani_samjichang.art_critique.member;

import com.google.firebase.database.annotations.NotNull;
import com.gorani_samjichang.art_critique.common.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

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

    public String readRole(String email) {
        MemberEntity member = memberRepository.findByEmail(email);
        return member.getRole();
    }

    public MemberDto CreateMember(String pw, String nickname, String level, MultipartFile profile) {
        return null;
    }

    public String emailTokenValidation(HttpServletRequest request) {
        String token = null;
        Cookie[] list = request.getCookies();
        if (list == null) {
            return null;
        }
        for (Cookie cookie : list) {
            if (cookie.getName().equals("token")) {
                token = cookie.getValue();
                break;
            }
        }
        if (token == null) {
            return null;
        }
        return jwtUtil.getEmail(token);
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
