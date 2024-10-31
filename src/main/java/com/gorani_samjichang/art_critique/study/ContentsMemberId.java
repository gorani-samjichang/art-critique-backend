package com.gorani_samjichang.art_critique.study;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ContentsMemberId implements Serializable {
    private Long contentsId;
    private Long memberId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentsMemberId that = (ContentsMemberId) o;
        return Objects.equals(memberId, that.memberId) && Objects.equals(contentsId, that.contentsId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, contentsId);
    }
}
