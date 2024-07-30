package com.gorani_samjichang.art_critique.credit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreditUsedHistoryRepository extends JpaRepository<CreditUsedHistoryEntity, Long> {

    @Query("select p from CreditUsedHistoryEntity p where p.memberEntity.uid = :uid order by p.usedDate")
    List<CreditUsedHistoryEntity> findAllByUid(@Param("uid") Long uid);
}
