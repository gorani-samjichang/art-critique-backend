package com.gorani_samjichang.art_critique.member;

import com.gorani_samjichang.art_critique.common.exceptions.UserNotFoundException;
import com.gorani_samjichang.art_critique.common.exceptions.UserNotValidException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberManager {
    private final MemberRepository memberRepository;

    public MemberEntity findValidMember(String email) {
        MemberEntity member = findMember(email);
        if (member.getIsDeleted()) {
            throw new UserNotValidException("Invalid Email");
        }
        return member;
    }

    public MemberEntity findMember(String email) {
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No Such a Email"));
        return member;
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    public boolean existsByNickname(String nickName) {
        return memberRepository.existsByNickname(nickName);
    }

    public void save(MemberEntity member) {
        memberRepository.save(member);
    }

}
