package com.gorani_samjichang.art_critique.study;

import com.gorani_samjichang.art_critique.feedback.FeedbackResultJSON;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExternalStudyContentsRepository extends JpaRepository<ExternalStudyContentEntity, Long> {
    @Query("select max(p.stamp) from ExternalStudyContentEntity p")
    Optional<Long> getStamp();

    Optional<ExternalStudyContentEntity> findByUrl(String url);

    @Query("select new com.gorani_samjichang.art_critique.study.StudyInfoDTO(s.author, s.title, s.url) " +
            " from ExternalStudyContentClassificationEntity c join ExternalStudyContentEntity s on c.content=s" +
            " where c.category= :keyword")
    List<StudyInfoDTO> getStudies(@Param("keyword") String keyword, Pageable pageable);

    @Query("select count(1) from ExternalStudyContentClassificationEntity c join ExternalStudyContentEntity s on c.content=s " +
            " where c.category= :keyword")
    int countStudies(@Param("keyword") String keyword);
}
