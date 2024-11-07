package com.gorani_samjichang.art_critique.study;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InnerContentsCommentRepository extends JpaRepository<InnerContentsComment, Long> {
    @Query("select new com.gorani_samjichang.art_critique.study.StudyCommentDTO(m.nickname, m.profile, cm.likes, cm.createdAt, cm.comment) " +
            "from InnerContentsComment cm " +
            "join cm.member m " +
            "order by cm.createdAt desc")
    List<StudyCommentDTO> getComments(Pageable pageable);
}
