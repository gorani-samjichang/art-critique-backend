package com.gorani_samjichang.art_critique.plan;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/plan")
public class PlanController {
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
}
