package com.gorani_samjichang.art_critique.study;

import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
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
    public List<SimpleInnerContentDTO> likesOrder(){
        return studyService.likesOrder();
    }

    @GetMapping("/public/category")
    public List<InnerStudyField> categories(){
        return studyService.categories();
    }

    @GetMapping("/public/{categoryThread}")
    public List<StudyCommentDTO> comments(@PathVariable String categoryThread){
        return studyService.getComments(categoryThread);
    }

    @PostMapping("/{categoryThread}")
    public void addComment(@PathVariable String categoryThread, @RequestParam("islike") Boolean islike, @RequestParam("content") String content, @AuthenticationPrincipal CustomUserDetails userDetail){
        studyService.writeComment(categoryThread, islike, content, userDetail.getSerialNumber());
    }

    @GetMapping("/public/categoryArticle/{studyField}/{studyCategory}/{level}")
    public List<InnerContentsCategoryDTO> getStudyDetails(@PathVariable Long studyField, @PathVariable Long studyCategory, @PathVariable String level){
        if("all".equals(level)){
            return studyService.getCategoryContents(studyField, studyCategory, PageRequest.of(0,6));
        }
        return studyService.getCategoryContentsWithLevel(studyField, studyCategory, level, PageRequest.of(0,6));
    }

    @GetMapping("/public/categoryArticle/{studyField}/{studyCategory}/{level}/{page}")
    public List<InnerContentsCategoryDTO> getStudyDetails(@PathVariable Long studyField, @PathVariable Long studyCategory, @PathVariable String level, @PathVariable Integer page){
        if("all".equals(level)){
            return studyService.getCategoryContents(studyField, studyCategory, PageRequest.of(page,6));
        }
        return studyService.getCategoryContentsWithLevel(studyField, studyCategory, level, PageRequest.of(page,6));
    }

    @GetMapping("/public/sameCategoryArticle/{field}/{category}")
    public List<SimpleInnerContentDTO> getCategoryContents(@PathVariable Long field, @PathVariable Long category){
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

    @GetMapping("sameTagArticle/{tag}")
    public List<SimpleInnerContentDTO> sameTagArticle(@PathVariable String tag) {
        return studyService.findByTag(tag);
    }
}
