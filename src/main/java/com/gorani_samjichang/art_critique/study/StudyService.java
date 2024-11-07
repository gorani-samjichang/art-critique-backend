package com.gorani_samjichang.art_critique.study;

import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.common.exceptions.BadRequestException;
import com.gorani_samjichang.art_critique.common.exceptions.CannotFindBySerialNumberException;
import com.gorani_samjichang.art_critique.common.exceptions.NoPermissionException;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyService {
    final InnerContentsRepository innerContentsRepository;
    final InnerStudyFieldRepository innerStudyFieldRepository;
    final InnerContentsCommentRepository commentRepository;
    final MemberRepository memberRepository;
    final InnerContentsLikesRepository likesRepository;
    final InnerStudyCategoryRepository studyCategoryRepository;
    final InnerContentsDetailsRepository contentsDetailsRepository;
    final CommonUtil commonUtil;

    public List<InnerContentsDTO> RecentOrder() {
        Pageable pageable = PageRequest.of(0, 5);
        return innerContentsRepository.findTop5ByOrderByCreatedAtDesc(pageable);
    }

    public List<SimpleInnerContentDTO> likesOrder() {
        return innerContentsRepository.findTop5ByOrderByLikesDesc(PageRequest.of(0, 5));
    }

    public List<InnerStudyFieldDTO> categories() {
        return innerStudyFieldRepository.findAll().stream().map(InnerStudyFieldDTO::new).toList();

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
        if (likesRepository.existsByContentsSerialNumberAndMemberSerialNumber(serialNumber, userSerialNumber)) {
            throw new BadRequestException("already liked");
        }
        likesRepository.insertLikes(serialNumber, userSerialNumber);
        innerContentsRepository.incrementLikes(serialNumber);
    }

    public List<SimpleInnerContentDTO> findByTag(String tag) {
        return innerContentsRepository.searchWithTag(tag);
    }

    public List<SimpleInnerContentDTO> findSameCategory(String serialNumber) {
        List<SimpleInnerContentDTO> dtoList = innerContentsRepository.searchWithCategory(serialNumber, PageRequest.of(0, 6)).stream()
                .filter(d -> !d.getArticleSerialNumber().equals(serialNumber))
                .collect(Collectors.toCollection(ArrayList::new));
        if (dtoList.size() == 6) {
            dtoList.remove(5);
        }
        return dtoList;
    }

    public InnerStudyCategory findCategory(Long field, Long subCategory) {
        InnerStudyCategory category = studyCategoryRepository.findTopByCategroyNum(subCategory).orElseThrow(() -> new CannotFindBySerialNumberException("category is not Exists"));
        if (category.getField().getCategoryNumber().equals(field)) {
            return category;
        }
        throw new CannotFindBySerialNumberException("category is not Exists");
    }

    public List<String> uploadImages(List<MultipartFile> files) {
        ArrayList<String> images = new ArrayList<>();
        String fileName;
        for (MultipartFile file : files) {
            fileName = UUID.randomUUID().toString();
            try {
                images.add(commonUtil.uploadToStorage(file, fileName));
            } catch (IOException e) {
                throw new NoPermissionException("Upload Server is Not Available.");
            }
        }

        return images;
    }

    @Transactional
    public void saveContent(String userSerialNumber, List<String> fileNames, ContentRequestDTO requestDTO, InnerStudyCategory category) {
        int index = 0;
        for (ArticleContent content : requestDTO.getArticleContent()) {
            if ("img".equals(content.getType())) {
                if (content.isThumnail()) break;
                index++;
            }
        }
        String thumbnailURL = fileNames.get(index);
        InnerContentsEntity innerContentsEntity = InnerContentsEntity.builder()
                .serialNumber(UUID.randomUUID().toString())
                .thumbnailUrl(thumbnailURL)
                .level(requestDTO.getLevel())
                .author(memberRepository.findBySerialNumber(userSerialNumber))
                .createdAt(LocalDateTime.now())
                .view(0L)
                .likes(0L)
                .title(requestDTO.getArticleTitle())
                .tags(requestDTO.getTagList())
                .subCategory(category)
                .build();
        innerContentsRepository.save(innerContentsEntity);
        index = 0;
        InnerContentsDetailsEntity detail;
        for (ArticleContent article : requestDTO.getArticleContent()) {
            if ("img".equals(article.getType())) {
                article.setContent(fileNames.get(index));
                index++;
            }
            detail = InnerContentsDetailsEntity.builder()
                    .type(article.getType())
                    .content(article.getContent())
                    .innerContents(innerContentsEntity)
                    .build();
            contentsDetailsRepository.save(detail);
        }
    }

    public void makeContent(String userSerialNumber, List<MultipartFile> files, ContentRequestDTO requestDTO) {
        InnerStudyCategory category = findCategory(requestDTO.getBigCategory(), requestDTO.getSmallCategory());
        List<String> fileNames = uploadImages(files);
        saveContent(userSerialNumber, fileNames, requestDTO, category);
    }

    public ContentsDetailResponseDTO getContentInfoWithDetails(String serialNumber) {
        ContentsDetailResponseDTO dto = innerContentsRepository.searchWithSerialNumberToContentsDetailResponseDTOWithoutDetails(serialNumber).orElseThrow(() -> new CannotFindBySerialNumberException("No article"));
        dto.getArticleMetaData().setTags(innerContentsRepository.getTags(serialNumber));
        dto.setArticleContent(innerContentsRepository.findArticleContentBySerialNumber(serialNumber));
        return dto;
    }

    public String getCategoryName(Long fieldNum, Long subCategoryNum){
        InnerStudyCategory category = studyCategoryRepository.findTopByCategroyNum(subCategoryNum).orElseThrow(()->new CannotFindBySerialNumberException("Category is not Exists"));
        if(category.getField().getCategoryNumber().equals(fieldNum)){
            return category.getCategoryName();
        }
        throw new CannotFindBySerialNumberException("study field is not Exists");
    }
}
