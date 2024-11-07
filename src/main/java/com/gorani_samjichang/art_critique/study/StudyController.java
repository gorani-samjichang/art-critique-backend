package com.gorani_samjichang.art_critique.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
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
    public void addComment(@RequestParam("islike") Boolean islike, @RequestParam("content") String content, @AuthenticationPrincipal CustomUserDetails userDetail) {
        studyService.writeComment(islike, content, userDetail.getSerialNumber());
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
        // 너무 빠르면 스크롤에 의한 추가 로딩이 실험이 안되서 넣은 임시 코드 지워야 됨
        try {
            Thread.sleep(2000); // 1초 동안 멈춤
        } catch (InterruptedException e) {
            e.printStackTrace(); // 예외 처리
        }
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
        System.out.println(userDetails.getSerialNumber());
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ContentRequestDTO contentRequestDTO = objectMapper.readValue(contentJson, ContentRequestDTO.class);
            System.out.println(contentRequestDTO);
            studyService.makeContent(userDetails.getSerialNumber(), imageFileList, contentRequestDTO);
        } catch (Exception e) {
            System.out.println("!!!" + e + "!!!");
        }
    }

    @GetMapping("/articleContent/{serialNumber}")
    public ContentsDetailResponseDTO getContentInfoWithDetails(@PathVariable String serialNumber) {
        return studyService.getContentInfoWithDetails(serialNumber);
    }

    @GetMapping("/public/categoryName/{fieldSerialNumber}/{subCategorySerialNumber}")
    public String getCategoryName(@PathVariable Long fieldSerialNumber, @PathVariable Long subCategorySerialNumber) {
        return studyService.getCategoryName(fieldSerialNumber, subCategorySerialNumber);
    }

    @GetMapping("/recommmendTag/{amount}")
    public List<String> getTagsRandom(@PathVariable int amount){
        return studyService.getTagsRandom(amount);
    }

    @GetMapping("/public/tagArticle/{tag}/{level}/{page}")
    public  List<InnerContentsCategoryDTO> tagArticleFinder(@PathVariable String tag, @PathVariable String level, @PathVariable int page) {
        return studyService.tagArticleFinder(tag, level, page);
    }

    @GetMapping("/myArticle/{page}")
    public List<InnerContentsCategoryDTO> myArticleFinder(@PathVariable int page,@AuthenticationPrincipal CustomUserDetails userDetails ) {
        return studyService.searchArticleWithMember(userDetails.getSerialNumber(), page);
    }
}
