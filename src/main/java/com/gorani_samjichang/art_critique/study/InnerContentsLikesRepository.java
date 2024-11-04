package com.gorani_samjichang.art_critique.study;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InnerContentsLikesRepository extends JpaRepository<ContentsLikes, Long> {

    @Modifying
    @Query(value = "INSERT INTO inner_contents_likes(cid, uid) VALUES (:contentSerialNumber, :memberSerialNumber)", nativeQuery =true)
    int insertLikes(@Param("contentSerialNumber") String contentSerialNumber, @Param("memberSerialNumber") String memberSerialNumber);

    boolean existsByContentsSerialNumberAndMemberSerialNumber(String contentSerialNumber, String memberSerialNumber);
}
