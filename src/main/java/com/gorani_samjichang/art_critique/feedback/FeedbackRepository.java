package com.gorani_samjichang.art_critique.feedback;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {
    Optional<FeedbackEntity> findBySerialNumber(String serialNumber);

    Slice<FeedbackEntity> findByMemberEntityUidAndIsHeadOrderByCreatedAtDesc(Long uid, boolean isHead, Pageable page);

    Slice<FeedbackEntity> findByMemberEntityUidAndIsHeadOrderByCreatedAtAsc(Long uid, boolean isHead, Pageable page);

    Slice<FeedbackEntity> findByMemberEntityUidAndIsHeadOrderByTotalScoreDesc(Long uid, boolean isHead, Pageable page);

    @Query("SELECT p FROM FeedbackEntity p WHERE p.isHead = true and p.memberEntity.uid = :uid and p.isBookmarked = true order by p.createdAt desc")
    List<FeedbackEntity> findByUidAndBookmarked(@Param("uid") Long uid);

    @Query("select p.serialNumber from FeedbackEntity p where p.serialNumber = :serialNumber")
    String getImageUrlBySerialNumber(@Param("serialNumber") String serialNumber);

    Slice<FeedbackEntity> findByMemberEntityUidAndIsBookmarkedAndIsHeadOrderByCreatedAtDesc(Long uid, Boolean isBookmarked, boolean isHead, Pageable page);

    @Query("select p.createdAt from FeedbackEntity p where p.memberEntity.uid = :uid")
    List<LocalDateTime> getFeedbackLogByUid(@Param("uid") Long uid);;

    @Query("select avg(p.totalScore) from FeedbackEntity p where p.memberEntity.uid = :uid")
    Long getAvgTotalScoreOfFeedbackByUid(@Param("uid") Long uid);
}
