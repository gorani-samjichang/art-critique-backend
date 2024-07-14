package com.gorani_samjichang.art_critique;

import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    final MemberRepository memberRepository;
    final BCryptPasswordEncoder bCryptPasswordEncoder;

    @PostConstruct
    void makeMember() {
        MemberEntity me = MemberEntity.builder().email("aa@aa.aa").password(bCryptPasswordEncoder.encode("aaaaaa")).credit(19).nickname("ggggg").role("USER").build();
        memberRepository.save(me);
    }

    @GetMapping("/hello")
    String hello() {
        return "hello";
    }
}
