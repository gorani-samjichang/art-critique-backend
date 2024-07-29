package com.gorani_samjichang.art_critique.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<MemberEntity> findByEmail(String email);

    MemberEntity findByEmailAndIsDeleted(String email, boolean b);

    MemberEntity findBySerialNumber(String serialNumber);
}