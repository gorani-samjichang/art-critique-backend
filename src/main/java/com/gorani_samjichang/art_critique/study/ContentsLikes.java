package com.gorani_samjichang.art_critique.study;

import com.gorani_samjichang.art_critique.member.MemberEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inner_contents_likes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentsLikes {
    @EmbeddedId
    private ContentsMemberId id;


    @ManyToOne
    @MapsId("contentsId")
    @JoinColumn(name = "cid")
    private InnerContentsEntity contents;


    @ManyToOne
    @MapsId("memberId")
    @JoinColumn(name = "uid")
    private MemberEntity member;

}
