package com.gorani_samjichang.art_critique.credit;

import com.gorani_samjichang.art_critique.appConstant.FeedbackState;
import com.gorani_samjichang.art_critique.feedback.FeedbackEntity;
import com.gorani_samjichang.art_critique.feedback.FeedbackResultEntity;
import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/credit")
@RequiredArgsConstructor
public class CreditController {
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
    String paymentResult(@PathVariable String paymentId) {
        String body = UriComponentsBuilder.newInstance()
                .queryParam("paymentId", paymentId)
                .build()
                .toUriString()
                .substring(1);
        String r = webClientBuilder.build()
                .post()
                .uri("https://dev.apis.naver.com/naverpay-partner/naverpay/payments/v2.2/apply/payment")
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .header("X-NaverPay-Chain-Id", chainId)
                .header("X-NaverPay-Idempotency-Key", String.valueOf(UUID.randomUUID()))
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return r;
    }

    @GetMapping("/sub/payment-result/{reserveId}/{tempReceiptId}")
    String paymentResult(@PathVariable String reserveId, @PathVariable String tempReceiptId) {
        String body = UriComponentsBuilder.newInstance()
                .queryParam("reserveId", reserveId)
                .queryParam("tempReceiptId", tempReceiptId)
                .build()
                .toUriString()
                .substring(1);
        String r = webClientBuilder.build()
                .post()
                .uri("https://dev.apis.naver.com/naverpay-partner/naverpay/payments/recurrent/regist/v1/approval")
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .header("X-NaverPay-Chain-Id", chainId)
                .header("X-NaverPay-Idempotency-Key", String.valueOf(UUID.randomUUID()))
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return r;
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
