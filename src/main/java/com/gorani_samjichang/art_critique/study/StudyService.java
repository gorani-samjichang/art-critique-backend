package com.gorani_samjichang.art_critique.study;

import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.common.exceptions.BadRequestException;
import com.gorani_samjichang.art_critique.common.exceptions.CannotFindBySerialNumberException;
import com.gorani_samjichang.art_critique.common.exceptions.NoPermissionException;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private final InnerContentsLikesRepository innerContentsLikesRepository;
    private List<String> tagPool = new ArrayList<>();

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

    public List<StudyCommentDTO> getComments(int pageNumber) {
        return commentRepository.getComments(PageRequest.of(pageNumber, 6));
    }

    public WriteCommentResponseDTO writeComment(boolean isLike, String replyString, String userSerialNumber) {
        InnerContentsComment comment = InnerContentsComment.builder()
                .comment(replyString)
                .member(memberRepository.findBySerialNumber(userSerialNumber))
                .likes(isLike)
                .createdAt(LocalDateTime.now())
                .build();
        commentRepository.save(comment);
        return WriteCommentResponseDTO.builder()
                .like(isLike)
                .memberName(comment.getMember().getNickname())
                .memberProfile(comment.getMember().getProfile())
                .content(replyString)
                .createdAt(comment.getCreatedAt())
                .build();
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
        if (tagPool.size() < 101) {
            updateTags();
        }
    }

    public ContentsDetailResponseDTO getContentInfoWithDetails(String serialNumber, String userSerialNumber) {
        ContentsDetailResponseDTO dto = innerContentsRepository.searchWithSerialNumberToContentsDetailResponseDTOWithoutDetails(serialNumber).orElseThrow(() -> new CannotFindBySerialNumberException("No article"));
        if(null != dto.getDeletedAt()){
            throw new BadRequestException("Deleted Contents are not allowed");
        }
        dto.getArticleMetaData().setTags(innerContentsRepository.getTags(serialNumber));
        dto.setArticleContent(innerContentsRepository.findArticleContentBySerialNumber(serialNumber));
        dto.getArticleMetaData().setAlreadyLike(innerContentsLikesRepository.existsByContentsSerialNumberAndMemberSerialNumber(serialNumber,userSerialNumber));
        return dto;
    }

    public String getCategoryName(Long fieldNum, Long subCategoryNum) {
        InnerStudyCategory category = studyCategoryRepository.findTopByCategroyNum(subCategoryNum).orElseThrow(() -> new CannotFindBySerialNumberException("Category is not Exists"));
        if (category.getField().getCategoryNumber().equals(fieldNum)) {
            return category.getCategoryName();
        }
        throw new CannotFindBySerialNumberException("study field is not Exists");
    }

    @Scheduled(fixedRate = 1000 * 60 * 60 * 24)
    public void updateTags() {
        List<String> tags = innerContentsRepository.findAllTags();
        Collections.shuffle(tags);
        tagPool = tags.subList(0, Math.min(101, tags.size()));
    }

    public List<String> getTagsRandom(int amount) {
        int poolSize = tagPool.size();
        if (amount >= poolSize) {
            return tagPool;
        }
        int startNum = (int) (Math.random() * (poolSize - amount + 1));
        return tagPool.subList(startNum, startNum + amount);
    }

    public List<InnerContentsCategoryDTO> tagArticleFinder(String tag, String level, int page) {
        if ("all".equals(level)) {
            return innerContentsRepository.searchInnerContentsWithTag(tag, PageRequest.of(page, 6));
        }
        return innerContentsRepository.searchInnerContentsWithTagAndLevel(tag, level, PageRequest.of(page, 6));
    }

    public List<InnerContentsCategoryDTO> searchArticleWithMember(String memberSerialNumber, int page) {
        return innerContentsRepository.searchWithMember(memberSerialNumber, PageRequest.of(page, 6));
    }
    public ArticleInfoDTO analyzeArticleInfo(String memberSerialNumber){
        ArticleInfoDTO dto = innerContentsRepository.analyzeInfoOf(memberSerialNumber);
        dto.setTags(innerContentsRepository.findAllTagsOfMember(memberSerialNumber));
        return dto;
    }

    public void deleteArticle(String memberSerialNumber, String contentSerialNumber){
        InnerContentsEntity contentsEntity = innerContentsRepository.findBySerialNumberAndAuthorSerialNumberAndDeletedAtIsNull(contentSerialNumber, memberSerialNumber).orElseThrow(()->new CannotFindBySerialNumberException("No article found"));
        contentsEntity.setDeletedAt(LocalDateTime.now());
        innerContentsRepository.save(contentsEntity);
        if (tagPool.size() < 101) {
            updateTags();
        }
    }
}
