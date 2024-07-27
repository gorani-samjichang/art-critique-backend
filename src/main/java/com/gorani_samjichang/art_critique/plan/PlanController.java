package com.gorani_samjichang.art_critique.plan;

import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/plan")
@RequiredArgsConstructor
public class PlanController {
    final MemberRepository memberRepository;
    List<PlanVo> prepaymentPlans = new ArrayList<>();
    List<PlanVo> subscribePlans = new ArrayList<>();
    @PostConstruct
    void init() {
        prepaymentPlans.add(PlanVo.builder().amount(5).totalCost(6524).costPerImage(624).build());
        prepaymentPlans.add(PlanVo.builder().amount(10).totalCost(7524).costPerImage(614).build());
        prepaymentPlans.add(PlanVo.builder().amount(15).totalCost(8524).costPerImage(604).build());
        prepaymentPlans.add(PlanVo.builder().amount(20).totalCost(9524).costPerImage(594).build());

        subscribePlans.add(PlanVo.builder().amount(5).totalCost(6524).costPerImage(624).build());
        subscribePlans.add(PlanVo.builder().amount(10).totalCost(7524).costPerImage(614).build());
        subscribePlans.add(PlanVo.builder().amount(15).totalCost(8524).costPerImage(604).build());
    }

    @GetMapping("/public/list")
    HashMap<String, List<PlanVo>> list() {
        HashMap<String, List<PlanVo>> dto = new HashMap<>();
        dto.put("prepaymentAmountOptionList", prepaymentPlans);
        dto.put("subscribeAmountOptionList", subscribePlans);
        return dto;
    }

    @PostMapping("/prepaymentSelect/{index}")
    ResponseEntity<Void> prepaymentSelect(@PathVariable String index, @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean payResult = isPaymentSuccess();

//        System.out.println(userDetails.getUsername());
//        System.out.println(userDetails.getSerialNumber());
//        System.out.println(userDetails.getRole());
        if (!payResult) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(501));
        }
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @PostMapping("/subscribeSelect/{index}/{autoPaymentOption}")
    ResponseEntity<Void> subscribeSelect(@PathVariable String index, @AuthenticationPrincipal UserDetails userDetails, @PathVariable Boolean autoPaymentOption) {
        boolean payResult = isPaymentSuccess();

        if (!payResult) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(501));
        }

        System.out.println(index + " " + autoPaymentOption);
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    private boolean isPaymentSuccess() {
        return true;
    }
}
