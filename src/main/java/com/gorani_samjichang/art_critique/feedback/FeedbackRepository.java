package com.gorani_samjichang.art_critique.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {
    Optional<FeedbackEntity> findBySerialNumber(String serialNumber);
    Slice<FeedbackEntity> findByMemberEntityUidOrderByCreatedAtDesc(Long uid, Pageable page);
    Slice<FeedbackEntity> findByMemberEntityUidOrderByCreatedAtAsc(Long uid, Pageable page);
    Slice<FeedbackEntity> findByMemberEntityUidOrderByTotalScoreDesc(Long uid, Pageable page);
    @Query("SELECT p FROM FeedbackEntity p WHERE p.isHead = true and p.memberEntity.uid = :uid and p.isBookmarked = true order by p.createdAt desc")
    List<FeedbackEntity> findByUidAndBookmarked(@Param("uid") Long uid);
    @Query("select p.serialNumber from FeedbackEntity p where p.serialNumber = :serialNumber")
    String getImageUrlBySerialNumber(@Param("serialNumber") String serialNumber);
    Slice<FeedbackEntity> findByMemberEntityUidAndIsBookmarkedOrderByCreatedAtDesc(Long uid, Boolean isBookmarked, Pageable page);
}
