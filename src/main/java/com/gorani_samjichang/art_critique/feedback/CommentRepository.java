package com.gorani_samjichang.art_critique.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<FeedbackCommentEntity, Long> {
    @Query("select NEW com.gorani_samjichang.art_critique.feedback.CommentDto(m.nickname, m.serialNumber, m.profile, c.createdAt, c.contents) from FeedbackCommentEntity c join c.memberEntity m where c.feedback.serialNumber= :serialNumber")
    List<CommentDto> findByFeedbackSerialNumberOrderByCreatedAtDesc(String serialNumber);

}
