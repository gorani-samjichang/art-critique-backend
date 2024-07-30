package com.gorani_samjichang.art_critique.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<MemberEntity> findByEmail(String email);

    Optional<MemberEntity> findByUid(long uid);

    MemberEntity findByEmailAndIsDeleted(String email, boolean b);

    MemberEntity findBySerialNumber(String serialNumber);

    @Query("select p.credit from MemberEntity p where p.uid = :uid")
    Integer getCreditByUid(@Param("uid") Long uid);
}
