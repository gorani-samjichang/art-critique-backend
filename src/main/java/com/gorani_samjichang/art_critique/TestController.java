package com.gorani_samjichang.art_critique;

import com.gorani_samjichang.art_critique.credit.CreditEntity;
import com.gorani_samjichang.art_critique.credit.CreditRepository;
import com.gorani_samjichang.art_critique.feedback.CommentRepository;
import com.gorani_samjichang.art_critique.feedback.FeedbackCommentEntity;
import com.gorani_samjichang.art_critique.feedback.FeedbackEntity;
import com.gorani_samjichang.art_critique.feedback.FeedbackRepository;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import com.gorani_samjichang.art_critique.study.InnerStudyCategory;
import com.gorani_samjichang.art_critique.study.InnerStudyCategoryRepository;
import com.gorani_samjichang.art_critique.study.InnerStudyField;
import com.gorani_samjichang.art_critique.study.InnerStudyFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
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
    final FeedbackRepository feedbackRepository;
    final CommentRepository commentRepository;
    final InnerStudyCategoryRepository innerStudyCategoryRepository;
    final InnerStudyFieldRepository innerStudyFieldRepository;

    @PostConstruct
    void makeMember() {
        if(!memberRepository.existsByEmail("admin@aa.aa")) {
            MemberEntity admin = MemberEntity.builder().email("admin@aa.aa").password("$2a$10$1cYjz6/0YN2yfme968RxPOA34BF9KAx4nMDVD3GWqLDsXJnx0j2Yu").open(true).serialNumber("e22e1-22r3f3f133-f14f4f4").isDeleted(false).credit(2).nickname("admin").role("ROLE_ADMIN").isDeleted(false).build();
            memberRepository.save(admin);
            CreditEntity c2 = CreditEntity.builder().memberEntity(admin).state("VALID").purchaseDate(LocalDateTime.now().minusMinutes(5)).expireDate(LocalDateTime.now().plusMonths(2).minusMinutes(5)).type("PREPAYMENT").remainAmount(2).purchaseAmount(2).usedAmount(0).build();
            admin.addCredit(c2);
            creditRepository.save(c2);
            memberRepository.save(admin);

            System.out.println(innerStudyFieldRepository.count());
            if (innerStudyFieldRepository.count() == 0 && innerStudyCategoryRepository.count() == 0) {
                InnerStudyField f = InnerStudyField.builder().categoryTitle("잘그리는 법").build();
                innerStudyFieldRepository.save(f);
                InnerStudyCategory c = InnerStudyCategory.builder().categoryName("빛과 그림자").categroyNum(1L).field(f).build();
                f.addDetail(c);

                innerStudyCategoryRepository.save(c);
                innerStudyFieldRepository.save(f);
            }
        }
    }

    @GetMapping("/hello")
    String hello() {
        return "hello!";
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

    // http요청을 닫아주는 코드를 찾아서 추가할 필요가 있음
    @Value("${feedback.server.host}")
    String feedbackHost;
    @GetMapping("/feedbackServerCheck")
    public String feedbackServerCheck() {
        System.out.println("파이썬 서버 점검 시작:" + feedbackHost);
        String res = webClientBuilder.build()
                .get()
                .uri(feedbackHost + "/hello")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(res);
        return res;
    }

    @GetMapping("/usedMemory")
    String memoryCheck() {
        // JVM 메모리 사용량
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long usedHeapMemory = memoryMXBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
        String heapMemory = String.valueOf(usedHeapMemory) + "MB";

        // EC2 전체 메모리 사용량
        StringBuilder sb = new StringBuilder();
        sb.append("사용된 힙메모리:").append(heapMemory).append("\n");
        try {
            Process process = Runtime.getRuntime().exec("free -m");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            reader.readLine();
            line = reader.readLine();
            sb.append(line).append("\n");
            String [] result = line.split("\\s+");
            System.out.println(Arrays.toString(result));
        } catch (Exception e) {
//            e.printStackTrace();
        }

        return sb.toString();
    }
}