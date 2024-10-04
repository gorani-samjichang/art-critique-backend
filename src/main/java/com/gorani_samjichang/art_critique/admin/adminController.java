package com.gorani_samjichang.art_critique.admin;

import com.gorani_samjichang.art_critique.member.MemberDto;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class adminController {
    final MemberRepository memberRepository;
    @GetMapping("/isAdmin")
    ResponseEntity<Void> isAmdin() {
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @GetMapping("/userList")
    ResponseEntity<List<MemberDto>> userList() {
        List<MemberDto> list = memberRepository.getAllMemberDto();
        return new ResponseEntity<>(list, HttpStatusCode.valueOf(200));
    }
}
