package com.gorani_samjichang.art_critique.member;

import com.gorani_samjichang.art_critique.feedback.FeedbackUrlDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<MemberEntity> findByEmail(String email);

    Optional<MemberEntity> findByUid(long uid);

    MemberEntity findByEmailAndIsDeleted(String email, boolean b);

    MemberEntity findBySerialNumber(String serialNumber);

    @Query("select p.credit from MemberEntity p where p.uid = :uid and p.isDeleted=false")
    Optional<Integer> getCreditByUid(@Param("uid") Long uid);

    @Query("select new com.gorani_samjichang.art_critique.member.MemberDto(p.email, p.profile, p.role, p.level, p.nickname, p.serialNumber)" +
            "from MemberEntity p")
    List<MemberDto> getAllMemberDto();
}
