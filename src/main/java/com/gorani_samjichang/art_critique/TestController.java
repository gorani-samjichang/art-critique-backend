package com.gorani_samjichang.art_critique;

import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    final MemberRepository memberRepository;
    final BCryptPasswordEncoder bCryptPasswordEncoder;

    @PostConstruct
    void makeMember() {
        MemberEntity me = MemberEntity.builder().email("aa@aa.aa").password(bCryptPasswordEncoder.encode("aaaaaa")).serialNumber("efe1-22r3f3f133-f14f4f4").isDeleted(false).credit(19).nickname("ggggg").role("USER").isDeleted(false).build();
        memberRepository.save(me);
    }

    @GetMapping("/hello")
    String hello() {
        return "hello";
    }

    @GetMapping("/progress")
    public SseEmitter getProgress() {
        SseEmitter emitter = new SseEmitter();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (int i = 0; i <= 10; i++) {
                    emitter.send("Progress: " + i + "0%");
                    TimeUnit.MILLISECONDS.sleep(10);
                }
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                System.out.println("!!!");
            }
        });
        return emitter;
    }
}
