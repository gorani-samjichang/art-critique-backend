package com.gorani_samjichang.art_critique.study;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InnerStudyCategoryRepository extends JpaRepository<InnerStudyCategory, Long> {
    Optional<InnerStudyCategory> findTopByCategroyNum(Long categoryNum);
}
