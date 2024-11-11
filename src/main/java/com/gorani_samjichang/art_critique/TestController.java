package com.gorani_samjichang.art_critique;

import com.gorani_samjichang.art_critique.credit.CreditEntity;
import com.gorani_samjichang.art_critique.credit.CreditRepository;
import com.gorani_samjichang.art_critique.feedback.CommentRepository;
import com.gorani_samjichang.art_critique.feedback.FeedbackRepository;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import com.gorani_samjichang.art_critique.study.InnerStudyCategory;
import com.gorani_samjichang.art_critique.study.InnerStudyCategoryRepository;
import com.gorani_samjichang.art_critique.study.InnerStudyField;
import com.gorani_samjichang.art_critique.study.InnerStudyFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
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
        if (!memberRepository.existsByEmail("admin@aa.aa")) {
            MemberEntity admin = MemberEntity.builder().email("admin@aa.aa").password("$2a$10$1cYjz6/0YN2yfme968RxPOA34BF9KAx4nMDVD3GWqLDsXJnx0j2Yu").open(true).serialNumber("e22e1-22r3f3f133-f14f4f4").isDeleted(false).credit(2).nickname("admin").role("ROLE_ADMIN").isDeleted(false).build();
            memberRepository.save(admin);
            CreditEntity c2 = CreditEntity.builder().memberEntity(admin).state("VALID").purchaseDate(LocalDateTime.now().minusMinutes(5)).expireDate(LocalDateTime.now().plusMonths(2).minusMinutes(5)).type("PREPAYMENT").remainAmount(2).purchaseAmount(2).usedAmount(0).build();
            admin.addCredit(c2);
            creditRepository.save(c2);
            memberRepository.save(admin);
        }
        log.info("{}개의 스터디 필드가 존재", innerStudyFieldRepository.count());
        if (innerStudyFieldRepository.count() == 0 && innerStudyCategoryRepository.count() == 0) {
            String[] fieldList = {
                    "좋은 드로잉을 위한 기본, 자세와 선",
                    "사실적인 드로잉을 위해 면과 명암을 다루는 기술",
                    "드로잉에서 가장 중요한 관찰의 원리와 올바른 관찰법",
                    "효과적이고 완벽한 드로잉을 위한 계측과 평면화",
                    "실전 드로잉을 위한 핵심 기법들"
            };
            String[][] categoryList = {
                    {
                            "자신에게 숨겨져 있는 드로잉 본능을 꺼내라",
                            "다양한 드로잉의 세상",
                            "안정된 자세를 만들기 위한 7가지 규칙",
                            "좋은 선을 만드는 연필 잡기 방법",
                            "자연스러운 선 긋기를 위한 관절의 사용법",
                            "직선과 곡선 긋기의 정석",
                            "필압을 느끼며 선 긋기",
                            "자연스러운 선을 구사하는 방법",
                            "선의 모든 것"
                    },
                    {
                            "그림의 소재를 선택할 때 주의할 점",
                            "윤곽선으로 그리기와 면으로 그리기",
                            "면에 대한 이해와 활용",
                            "명암을 다루는 기술"
                    },
                    {
                            "눈의 다양한 특성과 시각에 대한 올바른 이해",
                            "관찰에 대한 이해와 관찰력의 향상",
                            "두 가지 관찰법",
                            "보이는 대로 관찰하기",
                            "계측을 이용하여 보이는 대로 관찰하기",
                            "해체를 이용하여 보이는 대로 관찰하기",
                            "보이는 대로 관찰하는 방법에 대한 의문",
                            "보이는 대로 관찰하기의 장점과 한계",
                            "이해하며 관찰하기",
                            "이해하며 관찰하기로 드로잉하기",
                            "이해하며 관찰하기의 장점과 한계",
                            "보이는 대로 그리기 + 이해하며 그리기",
                    },
                    {
                            "계측의 기본 원리와 10가지 계측 기술",
                            "평면화란 무엇인가?",
                            "평면화의 원리와 효과를 정확히 이해하기",
                            "대상을 평면화하는 가장 쉬운 3가지 방법",
                    },
                    {
                            "그려야 할 것과 생략해도 되는 것을 구분하는 방법",
                            "효과적인 생략을 위한 5가지 기법",
                            "효과적인 강조를 위한 5가지 기법",
                            "대상의 윤곽선 처리",
                            "드로잉에 심미적 효과를 덧입히는 8가지 기법",
                    }
            };
            for (int i = 0; i < fieldList.length; i++) {
                InnerStudyField field = InnerStudyField.builder().categoryTitle(fieldList[i]).build();
                innerStudyFieldRepository.save(field);
                for (String j : categoryList[i]) {
                    InnerStudyCategory category = InnerStudyCategory.builder().categoryName(j).field(field).build();
                    innerStudyCategoryRepository.save(category);
                    field.addDetail(category);
                }
                innerStudyFieldRepository.save(field);
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
                log.warn("SseEmitter에서 발생!!! -- {}", e.getMessage());
            }
        });
        return emitter;
    }

    // http요청을 닫아주는 코드를 찾아서 추가할 필요가 있음
    @Value("${feedback.server.host}")
    String feedbackHost;

    @GetMapping("/feedbackServerCheck")
    public String feedbackServerCheck() {
        log.info("파이썬 서버 점검 시작: {}", feedbackHost);
        String res = webClientBuilder.build()
                .get()
                .uri(feedbackHost + "/hello")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("feedback server에 심각한 문제 발생: {}", errorBody);
                                        return Mono.error(new RuntimeException("Error response: " + errorBody));
                                    });
                        })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doFinally(signalType -> log.info("파이썬 서버 정상점검"))
                .block();
        log.info("점검 결과: {}", res);
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
            String[] result = line.split("\\s+");
            System.out.println(Arrays.toString(result));
            log.info(Arrays.toString(result));
        } catch (Exception e) {
//            e.printStackTrace();
        }

        return sb.toString();
    }
}