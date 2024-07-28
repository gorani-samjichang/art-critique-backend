package com.gorani_samjichang.art_critique.credit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CreditRepository  extends JpaRepository<CreditEntity, Long> {
    @Query("select p from CreditEntity p " +
            "where p.memberEntity.uid = :uid and p.state = 'VALID' " +
            "order by p.expireDate asc, CASE WHEN p.type = 'SUBSCRIBE' THEN 0 WHEN p.type = 'PREPAYMENT' THEN 1 ELSE 2 END ASC " +
            "limit 1"
    )
    CreditEntity usedCreditEntityByRequest(@Param("uid") Long uid);

    @Query("SELECT p FROM CreditEntity p WHERE p.expireDate > :currentTime")
    List<CreditEntity> selectExpiredCredits(@Param("currentTime") LocalDateTime currentTime);
}
