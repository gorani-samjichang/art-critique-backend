package com.gorani_samjichang.art_critique.credit;

import com.gorani_samjichang.art_critique.appConstant.CreditType;
import com.gorani_samjichang.art_critique.appConstant.FeedbackState;
import com.gorani_samjichang.art_critique.feedback.FeedbackEntity;
import com.gorani_samjichang.art_critique.feedback.FeedbackResultEntity;
import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/credit")
@RequiredArgsConstructor
public class CreditController {
    final MemberRepository memberRepository;
    final CreditRepository creditRepository;
    final CreditUsedHistoryRepository creditUsedHistoryRepository;
    final WebClient.Builder webClientBuilder;
    @GetMapping("/payment-history")
    HashMap<String, Object> paymentHistory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer remainPrepaymentCredit = creditRepository.sumOfPrepaymentCredit(userDetails.getUid());
        Integer remainSubscribeCredit = creditRepository.sumOfSubscribeCredit(userDetails.getUid());

        HashMap<String, Object> dto = new HashMap<>();
        dto.put("remainPrepaymentCredit", remainPrepaymentCredit == null ? 0 : remainPrepaymentCredit);
        dto.put("remainSubscribeCredit", remainSubscribeCredit == null ? 0 : remainSubscribeCredit);
        List<CreditEntity> entities = creditRepository.findAllByUid(userDetails.getUid());
        List<CreditDto> list = new ArrayList<>();
        for (CreditEntity c : entities) {
            list.add(convertEntityToDto(c));
        }
        dto.put("paymentList", list);
        return dto;
    }

    @GetMapping("/usage-history")
    List<HashMap<String, Object>> usageHistory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CreditUsedHistoryEntity> history =  creditUsedHistoryRepository.findAllByUid(userDetails.getUid());

        List<HashMap<String, Object>> dto = new ArrayList<>();

        for (CreditUsedHistoryEntity h : history) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("usedDate", h.getUsedDate());
            map.put("imageUrl", h.getFeedbackEntity().getPictureUrl());
            map.put("serialNumber", h.getFeedbackEntity().getSerialNumber());
            map.put("type", h.getType());
            dto.add(map);
        }

        return dto;
    }

    @Value("${naver_pay.client_id}")
    String clientId;
    @Value("${naver_pay.client_secret}")
    String clientSecret;
    @Value("${naver_pay.chain_id}")
    String chainId;
    @GetMapping("/pre/payment-result/{paymentId}")
    Map paymentResult(@PathVariable String paymentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        String body = UriComponentsBuilder.newInstance()
                .queryParam("paymentId", paymentId)
                .build()
                .toUriString()
                .substring(1);
        Map r = webClientBuilder.build()
                .post()
                .uri("https://dev.apis.naver.com/naverpay-partner/naverpay/payments/v2.2/apply/payment")
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .header("X-NaverPay-Chain-Id", chainId)
                .header("X-NaverPay-Idempotency-Key", String.valueOf(UUID.randomUUID()))
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if("Success".equals(r.get("code"))) {
            String [] productName = ((String)((Map)((Map)r.get("body")).get("detail")).get("productName")).split(" ");
            int count = Integer.parseInt(productName[productName.length - 1].replace("개", "").trim());
            buyCredit(CreditType.PREPAYMENT, count, userDetails.getUid());
        }
        return r;
    }

    @GetMapping("/sub/payment-result/{reserveId}/{tempReceiptId}")
    Map paymentResult(@PathVariable String reserveId, @PathVariable String tempReceiptId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        String body = UriComponentsBuilder.newInstance()
                .queryParam("reserveId", reserveId)
                .queryParam("tempReceiptId", tempReceiptId)
                .build()
                .toUriString()
                .substring(1);
        Map r = webClientBuilder.build()
                .post()
                .uri("https://dev.apis.naver.com/naverpay-partner/naverpay/payments/recurrent/regist/v1/approval")
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .header("X-NaverPay-Chain-Id", chainId)
                .header("X-NaverPay-Idempotency-Key", String.valueOf(UUID.randomUUID()))
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if("Success".equals(r.get("code"))) {
            String recurrentId = (String)((Map)r.get("body")).get("recurrentId");
            MemberEntity myEntity = memberRepository.findById(userDetails.getUid()).get();
            myEntity.setNaverPayRecurrentId(recurrentId);
            memberRepository.save(myEntity);
        }
        return r;
    }
    @GetMapping("/subscribeInfo")
    Map isSubScribeInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MemberEntity myEntity = memberRepository.findById(userDetails.getUid()).get();
        String recurrentId = myEntity.getNaverPayRecurrentId();
        Map r = new HashMap<String, String>();
        if (recurrentId == null) {
            r.put("state", "no_info");
            return r;
        }
        r.put("state", "has_info");

        String body = "{\"recurrentId\": \"" + recurrentId + "\"}";
        Map v = webClientBuilder.build()
                .post()
                .uri("https://dev.apis.naver.com/naverpay-partner/naverpay/payments/recurrent/v1/list")
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .header("X-NaverPay-Chain-Id", chainId)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        r.put("content", v);
        return r;
    }

    @GetMapping("/sub/cancel")
    ResponseEntity<Void> cancelSubscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MemberEntity myEntity = memberRepository.findById(userDetails.getUid()).get();
        String recurrentId = myEntity.getNaverPayRecurrentId();
        if (recurrentId == null) return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        String body = UriComponentsBuilder.newInstance()
                .queryParam("expireRequester", "아트크리틱")
                .queryParam("expireReason", "단순변심")
                .build()
                .toUriString()
                .substring(1);
        Map r = webClientBuilder.build()
                .post()
                .uri("https://dev.apis.naver.com/naverpay-partner/naverpay/payments/recurrent/expire/v1/request?recurrentId=" + recurrentId)
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .header("X-NaverPay-Chain-Id", chainId)
                .header("X-NaverPay-Idempotency-Key", String.valueOf(UUID.randomUUID()))
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if ("Success".equals(r.get("code"))) {
            myEntity.setNaverPayRecurrentId(null);
        }
        memberRepository.save(myEntity);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    void buyCredit(String type, int count, Long uid) {
        MemberEntity myEntity = memberRepository.findById(uid).get();
        myEntity.setCredit(myEntity.getCredit() + count);
        CreditEntity creditEntity = CreditEntity.builder()
                .type(type)
                .memberEntity(myEntity)
                .state("VALID")
                .purchaseDate(LocalDateTime.now())
                .expireDate(LocalDateTime.now().plusYears(2))
                .usedAmount(0)
                .purchaseAmount(count)
                .remainAmount(count)
                .build();
        creditRepository.save(creditEntity);
        myEntity.addCredit(creditEntity);
        memberRepository.save(myEntity);
    }

    CreditDto convertEntityToDto(CreditEntity entity) {
        return CreditDto.builder()
                .usedAmount(entity.getUsedAmount())
                .state(entity.getState())
                .type(entity.getType())
                .purchaseAmount(entity.getPurchaseAmount())
                .remainAmount(entity.getRemainAmount())
                .expireDate(entity.getExpireDate())
                .purchaseDate(entity.getPurchaseDate())
                .build();
    }
}
