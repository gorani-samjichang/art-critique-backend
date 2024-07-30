package com.gorani_samjichang.art_critique.credit;

import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/credit")
@RequiredArgsConstructor
public class CreditController {
    final CreditRepository creditRepository;
    final CreditUsedHistoryRepository creditUsedHistoryRepository;
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
