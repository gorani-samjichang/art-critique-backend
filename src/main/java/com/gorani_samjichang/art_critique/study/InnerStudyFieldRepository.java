package com.gorani_samjichang.art_critique.study;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InnerStudyFieldRepository extends JpaRepository<InnerStudyField, Long> {
    @Override
    @EntityGraph(attributePaths = {"detail"})
    List<InnerStudyField> findAll();
}
