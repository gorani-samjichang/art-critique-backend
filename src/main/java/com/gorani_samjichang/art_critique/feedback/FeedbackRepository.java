package com.gorani_samjichang.art_critique.feedback;

import com.gorani_samjichang.art_critique.member.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {
}
