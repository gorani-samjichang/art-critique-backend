package com.gorani_samjichang.art_critique.study;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ExternalStudyContentsRepository extends JpaRepository<ExternalStudyContentEntity, Long> {
    @Query("select max(p.stamp) from ExternalStudyContentEntity p")
    Optional<Long> getStamp();

    Optional<ExternalStudyContentEntity> findByUrl(String url);
}
