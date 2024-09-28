package com.gorani_samjichang.art_critique;

import com.gorani_samjichang.art_critique.credit.CreditEntity;
import com.gorani_samjichang.art_critique.credit.CreditRepository;
import com.gorani_samjichang.art_critique.feedback.FeedbackEntity;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    final MemberRepository memberRepository;
    final CreditRepository creditRepository;
    final BCryptPasswordEncoder bCryptPasswordEncoder;
    final WebClient.Builder webClientBuilder;

    @PostConstruct
    void makeMember() {

        MemberEntity me = MemberEntity.builder().email("aa@aa.aa").password(bCryptPasswordEncoder.encode("aaaaaa")).open(true).serialNumber("efe1-22r3f3f133-f14f4f4").isDeleted(false).credit(2).nickname("ggggg").role("ROLE_USER").isDeleted(false).build();
        memberRepository.save(me);
        CreditEntity c1 = CreditEntity.builder().memberEntity(me).state("VALID").purchaseDate(LocalDateTime.now().minusMinutes(5)).expireDate(LocalDateTime.now().plusMonths(2).minusMinutes(5)).type("PREPAYMENT").remainAmount(2).purchaseAmount(2).usedAmount(0).build();
        me.addCredit(c1);
        creditRepository.save(c1);
        memberRepository.save(me);


        MemberEntity admin = MemberEntity.builder().email("admin@aa.aa").password(bCryptPasswordEncoder.encode("aaaaaa")).open(true).serialNumber("e22e1-22r3f3f133-f14f4f4").isDeleted(false).credit(2).nickname("admin").role("ROLE_ADMIN").isDeleted(false).build();
        memberRepository.save(admin);
        CreditEntity c2 = CreditEntity.builder().memberEntity(me).state("VALID").purchaseDate(LocalDateTime.now().minusMinutes(5)).expireDate(LocalDateTime.now().plusMonths(2).minusMinutes(5)).type("PREPAYMENT").remainAmount(2).purchaseAmount(2).usedAmount(0).build();
        admin.addCredit(c2);
        creditRepository.save(c2);
        memberRepository.save(admin);
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

    @Value("${feedback.server.host}")
    String feedbackHost;
    @GetMapping("/feedbackServerCheck")
    public String feedbackServerCheck() {
        String res = webClientBuilder.build()
                .get()
                .uri(feedbackHost + "/hello")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(res);
        return res;
    }
}
