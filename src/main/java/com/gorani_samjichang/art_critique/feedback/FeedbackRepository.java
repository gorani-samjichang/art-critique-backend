package com.gorani_samjichang.art_critique.feedback;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {
    Optional<FeedbackEntity> findBySerialNumber(String serialNumber);

    Slice<FeedbackEntity> findByMemberEntityUidAndStateNotOrderByCreatedAtDesc(Long uid, String state, Pageable page);

    Slice<FeedbackEntity> findByMemberEntityUidAndStateNotOrderByCreatedAtAsc(Long uid, String state, Pageable page);

    Slice<FeedbackEntity> findByMemberEntityUidAndStateNotOrderByTotalScoreDesc(Long uid, String state, Pageable page);

    List<FeedbackEntity> findByMemberEntityUidAndStateAndCreatedAtAfterOrderByCreatedAtAsc(Long uid, String state, LocalDateTime date);

    @Query("SELECT p FROM FeedbackEntity p WHERE p.memberEntity.uid = :uid and p.isBookmarked = true order by p.createdAt desc")
    List<FeedbackEntity> findByUidAndBookmarked(@Param("uid") Long uid);

    @Query("select p.serialNumber from FeedbackEntity p where p.serialNumber = :serialNumber")
    String getImageUrlBySerialNumber(@Param("serialNumber") String serialNumber);

    Slice<FeedbackEntity> findByMemberEntityUidAndIsBookmarkedOrderByCreatedAtDesc(Long uid, Boolean isBookmarked, Pageable page);

    @Query("select p.createdAt from FeedbackEntity p where p.state='COMPLETED' and p.memberEntity.uid = :uid")
    List<LocalDateTime> getFeedbackLogByUid(@Param("uid") Long uid);

    @Query("select avg(p.totalScore) from FeedbackEntity p where p.state='COMPLETED' and p.memberEntity.uid = :uid")
    Long getAvgTotalScoreOfFeedbackByUid(@Param("uid") Long uid);

    @Query("select new com.gorani_samjichang.art_critique.feedback.FeedbackUrlDto(p.pictureUrl, p.serialNumber) from FeedbackEntity p where p.memberEntity.serialNumber = :serialNumber and p.memberEntity.open = true and p.state='COMPLETED' and p.isPublic = true order by p.createdAt desc")
    List<FeedbackUrlDto> findAllserialNumberAndPictureUrlByMemberEntityUid(@Param("serialNumber") String serialNumber);

    @Query("select new com.gorani_samjichang.art_critique.feedback.FeedbackUrlDto(p.pictureUrl, p.serialNumber) from FeedbackEntity p where p.memberEntity.open = true and p.state='COMPLETED' and p.isPublic = true order by p.totalScore desc limit 10")
    List<FeedbackUrlDto> findGoodImage();
}
