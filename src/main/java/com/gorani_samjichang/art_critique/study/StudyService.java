package com.gorani_samjichang.art_critique.study;

import com.gorani_samjichang.art_critique.common.exceptions.CannotFindBySerialNumberException;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyService {
    final InnerContentsRepository innerContentsRepository;
    final InnerStudyFieldRepository innerStudyFieldRepository;
    final InnerContentsCommentRepository commentRepository;
    final MemberRepository memberRepository;
    final InnerContentsLikesRepository likesRepository;

    public List<InnerContentsDTO> RecentOrder() {
        Pageable pageable = PageRequest.of(0, 5);
        return innerContentsRepository.findTop5ByOrderByCreatedAtDesc(pageable);
    }

    public List<SimpleInnerContentDTO> likesOrder() {
        return innerContentsRepository.findTop5ByOrderByLikesDesc(PageRequest.of(0, 5));
    }

    public List<InnerStudyField> categories() {
        return innerStudyFieldRepository.findAll();
    }

    public List<StudyCommentDTO> getComments(String serialNumber) {
        return commentRepository.getComments(serialNumber);
    }

    public void writeComment(String contents, boolean isLike, String replyString, String userSerialNumber) {
        InnerContentsComment comment = InnerContentsComment.builder()
                .comment(replyString)
                .member(memberRepository.findBySerialNumber(userSerialNumber))
                .likes(isLike)
                .createdAt(LocalDateTime.now())
                .content(innerContentsRepository.findBySerialNumber(contents).orElseThrow(() -> new CannotFindBySerialNumberException("contents not exists")))
                .build();
        commentRepository.save(comment);
    }

    public List<InnerContentsCategoryDTO> getCategoryContents(long fieldNum, long subCategoryNum, Pageable page) {
        return innerContentsRepository.searchInnerContentsWithCategory(fieldNum, subCategoryNum, page);
    }

    public List<InnerContentsCategoryDTO> getCategoryContentsWithLevel(long fieldNum, long subCategoryNum, String level, Pageable page) {
        return innerContentsRepository.searchInnerContentsWithCategoryAndLevel(fieldNum, subCategoryNum, level, page);
    }

    public List<SimpleInnerContentDTO> findCategoryContents(long fieldNum, long subCategoryNum) {
        return innerContentsRepository.findCategoryArticles(fieldNum, subCategoryNum, PageRequest.of(0, 5));
    }

    public void increaseView(String serialNumber) {
        innerContentsRepository.incrementView(serialNumber);
    }

    @Transactional
    public void registerLikes(String serialNumber, String userSerialNumber) {
        if (!likesRepository.existsByContentsSerialNumberAndMemberSerialNumber(serialNumber, userSerialNumber)) {
            likesRepository.insertLikes(serialNumber, userSerialNumber);
            innerContentsRepository.incrementLikes(serialNumber);
        }
    }

    public List<SimpleInnerContentDTO> findByTag(String tag) {
        return innerContentsRepository.searchWithTag(tag);
    }
}
