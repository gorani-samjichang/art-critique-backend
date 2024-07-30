package com.gorani_samjichang.art_critique.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    MemberEntity findByEmail(String email);

    MemberEntity findByEmailAndIsDeleted(String email, boolean b);

    MemberEntity findBySerialNumber(String serialNumber);

    @Query("select p.credit from MemberEntity p where p.uid = :uid")
    Integer getCreditByUid(@Param("uid") Long uid);
}
