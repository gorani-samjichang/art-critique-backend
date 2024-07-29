package com.gorani_samjichang.art_critique.feedback;

import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {
    Optional<FeedbackEntity> findBySerialNumber(String serialNumber);

    @Query("SELECT p FROM FeedbackEntity p WHERE p.isHead = true and p.memberEntity.uid = :uid order by p.createdAt desc")
    List<FeedbackEntity> findByRecentOrder(@Param("uid") Long uid);
    @Query("SELECT p FROM FeedbackEntity p WHERE p.isHead = true and p.memberEntity.uid = :uid order by p.createdAt asc")
    List<FeedbackEntity> findByWriteOrder(@Param("uid") Long uid);
    @Query("SELECT p FROM FeedbackEntity p WHERE p.isHead = true and p.memberEntity.uid = :uid order by p.totalScore desc")
    List<FeedbackEntity> findByScoreOrder(@Param("uid") Long uid);
    @Query("SELECT p FROM FeedbackEntity p WHERE p.isHead = true and p.memberEntity.uid = :uid and p.isBookmarked = true order by p.createdAt desc")
    List<FeedbackEntity> findByUidAndBookmarked(@Param("uid") Long uid);
}
