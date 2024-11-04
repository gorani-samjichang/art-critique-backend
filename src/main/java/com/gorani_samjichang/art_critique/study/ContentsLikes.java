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
    @MapsId("contentsSerialNumber")
    @JoinColumn(name = "cid", referencedColumnName = "serialNumber")
    private InnerContentsEntity contents;


    @ManyToOne
    @MapsId("memberSerialNumber")
    @JoinColumn(name = "uid", referencedColumnName = "serialNumber")
    private MemberEntity member;

}
