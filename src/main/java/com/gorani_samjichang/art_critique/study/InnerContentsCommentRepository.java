package com.gorani_samjichang.art_critique.study;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InnerContentsCommentRepository extends JpaRepository<InnerContentsComment, Long> {
    @Query("select new com.gorani_samjichang.art_critique.study.StudyCommentDTO(m.nickname, m.profile, cm.likes, cm.createdAt) " +
            "from InnerContentsComment cm " +
            "join cm.member m " +
            "where cm.content.serialNumber = :serialNumber " +
            "order by cm.createdAt desc")
    List<StudyCommentDTO> getComments(@Param("serialNumber") String serialNumber);
}
