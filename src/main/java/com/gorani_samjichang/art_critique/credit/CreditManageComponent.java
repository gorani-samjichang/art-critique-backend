package com.gorani_samjichang.art_critique.credit;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class CreditManageComponent {
    final CreditRepository creditRepository;
    @Scheduled(cron = "0 0 0 * * *")
    public void expiringCredit() {
        List<CreditEntity> usedCredits = creditRepository.selectExpiredCredits(LocalDateTime.now());
        for (CreditEntity c : usedCredits) {
            c.setState("EXPIRED");
        }
    }
}
