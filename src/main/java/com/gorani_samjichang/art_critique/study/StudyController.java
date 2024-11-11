package com.gorani_samjichang.art_critique.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
@Slf4j
public class StudyController {
    final StudyService studyService;

    @GetMapping("/public/recentOrder")
    public List<InnerContentsDTO> recentOrder() {
        return studyService.RecentOrder();
    }

    @GetMapping("/public/likeOrder")
    public List<SimpleInnerContentDTO> likesOrder() {
        return studyService.likesOrder();
    }

    @GetMapping("/public/category")
    public List<InnerStudyFieldDTO> categories() {
        return studyService.categories();
    }

    @GetMapping("/public/categoryThread")
    public List<StudyCommentDTO> comments() {
        return studyService.getComments(0);
    }

    @GetMapping("/public/categoryThread/{pageNumber}")
    public List<StudyCommentDTO> comments(@PathVariable int pageNumber) {
        return studyService.getComments(pageNumber);
    }


    @PostMapping("/ctegoryThread")
    public WriteCommentResponseDTO addComment(@RequestParam("islike") Boolean islike, @RequestParam("content") String content, @AuthenticationPrincipal CustomUserDetails userDetail) {
        return studyService.writeComment(islike, content, userDetail.getSerialNumber());
    }

    @GetMapping("/public/categoryArticle/{studyField}/{studyCategory}/{level}")
    public List<InnerContentsCategoryDTO> getStudyDetails(@PathVariable Long studyField, @PathVariable Long studyCategory, @PathVariable String level) {
        if ("all".equals(level)) {
            return studyService.getCategoryContents(studyField, studyCategory, PageRequest.of(0, 6));
        }
        return studyService.getCategoryContentsWithLevel(studyField, studyCategory, level, PageRequest.of(0, 6));
    }

    @GetMapping("/public/categoryArticle/{studyField}/{studyCategory}/{level}/{page}")
    public List<InnerContentsCategoryDTO> getStudyDetails(@PathVariable Long studyField, @PathVariable Long studyCategory, @PathVariable String level, @PathVariable Integer page) {
        if ("all".equals(level)) {
            return studyService.getCategoryContents(studyField, studyCategory, PageRequest.of(page, 6));
        }
        return studyService.getCategoryContentsWithLevel(studyField, studyCategory, level, PageRequest.of(page, 6));
    }

    @GetMapping("/public/sameCategoryArticle/{field}/{category}")
    public List<SimpleInnerContentDTO> getCategoryContents(@PathVariable Long field, @PathVariable Long category) {
        return studyService.findCategoryContents(field, category);
    }

    @GetMapping("/articleView/{serialNumber}")
    public void increaseView(@PathVariable String serialNumber, @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyService.increaseView(serialNumber);
    }

    @GetMapping("/articleLike/{serialNumber}")
    public void registerLikes(@PathVariable String serialNumber, @AuthenticationPrincipal CustomUserDetails userDetails) {
        studyService.registerLikes(serialNumber, userDetails.getSerialNumber());
    }

    @GetMapping("/public/sameTagArticle/{tag}")
    public List<SimpleInnerContentDTO> sameTagArticle(@PathVariable String tag) {
        return studyService.findByTag(tag);
    }

    @GetMapping("/public/sameCategoryArticle/{serialNumber}")
    public List<SimpleInnerContentDTO> sameCategoryArticle(@PathVariable String serialNumber) {
        return studyService.findSameCategory(serialNumber);
    }

    @PostMapping("/makeContent")
    public void makeStudyContent(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestPart(value = "ImageFileList") List<MultipartFile> imageFileList,
                                 @RequestPart(value = "Content") String contentJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ContentRequestDTO contentRequestDTO = objectMapper.readValue(contentJson, ContentRequestDTO.class);
            studyService.makeContent(userDetails.getSerialNumber(), imageFileList, contentRequestDTO);
        } catch (Exception e) {
            log.error("컨텐츠 생성 실패, {}", contentJson);
        }
    }

    @GetMapping("/articleContent/{serialNumber}")
    public ContentsDetailResponseDTO getContentInfoWithDetails(@PathVariable String serialNumber, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return studyService.getContentInfoWithDetails(serialNumber, userDetails.getSerialNumber());
    }

    @GetMapping("/public/categoryName/{fieldSerialNumber}/{subCategorySerialNumber}")
    public String getCategoryName(@PathVariable Long fieldSerialNumber, @PathVariable Long subCategorySerialNumber) {
        return studyService.getCategoryName(fieldSerialNumber, subCategorySerialNumber);
    }

    @GetMapping("/public/recommmendTag/{amount}")
    public List<String> getTagsRandom(@PathVariable int amount) {
        return studyService.getTagsRandom(amount);
    }

    @GetMapping("/public/tagArticle/{tag}/{level}/{page}")
    public List<InnerContentsCategoryDTO> tagArticleFinder(@PathVariable String tag, @PathVariable String level, @PathVariable int page) {
        return studyService.tagArticleFinder(tag, level, page);
    }

    @GetMapping("/myArticle/{page}")
    public List<InnerContentsCategoryDTO> myArticleFinder(@PathVariable int page, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return studyService.searchArticleWithMember(userDetails.getSerialNumber(), page);
    }

    @GetMapping("/myArticleInfo")
    public ArticleInfoDTO myArticleInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return studyService.analyzeArticleInfo(userDetails.getSerialNumber());
    }

    @DeleteMapping("/myArticle/{serialNumber}")
    public void deleteArticle(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String serialNumber) {
        studyService.deleteArticle(userDetails.getSerialNumber(), serialNumber);
    }
}
