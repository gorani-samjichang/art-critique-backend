package com.gorani_samjichang.art_critique.feedback;

import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {
    Optional<FeedbackEntity> findBySerialNumber(String serialNumber);
    Slice<FeedbackEntity> findByMemberEntityEmail(String email, Pageable pageable);
}
