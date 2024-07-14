package com.gorani_samjichang.art_critique.feedback;

import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    final FeedbackRepository feedbackRepository;
    final MemberRepository memberRepository;
    final CommonUtil commonUtil;
    final BCryptPasswordEncoder bCryptPasswordEncoder;
    String [] dummyTodayGoodImage = new String [] {
            "https://picsum.photos/id/88/180/160",
            "https://picsum.photos/id/51/200/200",
            "https://picsum.photos/id/50/260/240",
            "https://picsum.photos/id/9/180/160",
            "https://picsum.photos/id/55/260/280",
            "https://picsum.photos/id/70/220/220",
            "https://picsum.photos/id/57/160/300",
            "https://picsum.photos/id/19/300/120",
            "https://picsum.photos/id/99/100/100",
            "https://picsum.photos/id/26/300/260",
            "https://picsum.photos/id/71/120/120",
            "https://picsum.photos/id/69/160/160",
            "https://picsum.photos/id/39/100/120",
            "https://picsum.photos/id/27/240/160",
            "https://picsum.photos/id/15/240/140"
    };

    @GetMapping("/public/good-image")
    String [] getGoodImage() {
        String [] todayGoodImage = dummyTodayGoodImage;
        return todayGoodImage;
    }

    @PostMapping("/request")
    String requestFeedback(@RequestParam("image") MultipartFile imageFile, HttpServletRequest request) throws IOException, InterruptedException {
        Thread.sleep(1500);

        FeedbackResultEntity feedbackResultEntity1 = FeedbackResultEntity.builder()
                .feedbackType("종합평가")
                .feedbackContent("{\"content\": \"이 그림은 전체적으로 높은 수준의 기술과 창의성을 보여주지만, 일부 세부적인 부분에서 개선할 수 있는 여지가 있습니다. 비율과 해부학적 정확성을 조금 더 현실감 있게 조절하고, 동작과 자세를 더욱 역동적으로 표현하며, 디테일과 정확성을 더 높이고, 조명과 그림자의 사용을 더욱 정교하게 하며, 색채와 채색의 깊이를 더욱 다채롭게 만들면 그림의 완성도가 한층 더 높아질 것입니다.\", \"class\": \"중수\"}")
                .build();

        FeedbackResultEntity feedbackResultEntity2 = FeedbackResultEntity.builder()
                .feedbackType("점수")
                .feedbackContent("{\"item\": [{\"itemName\": \"인체이해도\",\"itemGrade\": 55},{\"itemName\": \"구도와 표현력\",\"itemGrade\": 38},{\"itemName\": \"명암과 그림자\",\"itemGrade\": 26},{\"itemName\": \"비례와 원근\",\"itemGrade\": 52},{\"itemName\": \"색체 이해도\",\"itemGrade\": 83}]}")
                .build();

        FeedbackEntity feedbackEntity = FeedbackEntity.builder()
                .createdAt(LocalDateTime.now())
                .isPublic(true)
                .usedToken(10L)
                .version(1)
                .build();

        String serialNumber = "a" + bCryptPasswordEncoder.encode(String.valueOf(feedbackEntity.getFid()));
        serialNumber = serialNumber.replace("$", "-");
        serialNumber = serialNumber.replace("/", "_");
        serialNumber = serialNumber.replace(".", "Z");
        System.out.println(serialNumber);
        feedbackEntity.setSerialNumber(serialNumber);
        String imageUrl = commonUtil.uploadToStorage(imageFile, serialNumber);
        feedbackEntity.setPictureUrl(imageUrl);

        feedbackEntity.addFeedbackResult(feedbackResultEntity1);
        feedbackEntity.addFeedbackResult(feedbackResultEntity2);
        feedbackRepository.save(feedbackEntity);

        MemberEntity me = memberRepository.findByEmail(String.valueOf(request.getAttribute("email")));
        me.addFeedback(feedbackEntity);

        memberRepository.save(me);

        return feedbackEntity.getSerialNumber();
    }

    @GetMapping("/public/retrieve/{serialNumber}")
    FeedbackEntity retrieveFeedback(@PathVariable("serialNumber") String serialNumber) {
        Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
        if (feedbackEntity.isPresent()) {
            System.out.println("잘받음");
            return feedbackEntity.get();
        } else {
            System.out.println("아예 피드백이 없음");
            return null;
        }
    }

    @PostMapping("review/{serialNumber}")
    boolean feedbackReview(@PathVariable String serialNumber,
                           @RequestParam boolean isLike,
                           @RequestParam String review,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        if (review.length() < 9) {
            response.setStatus(401);
            return false;
        }
        Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
        if (feedbackEntity.isEmpty()) {
            response.setStatus(401);
        } else {
            if (!feedbackEntity.get().getMemberEntity().getEmail().equals(request.getAttribute("email"))) {
                response.setStatus(401);
                return false;
            }
            feedbackEntity.get().setUserReview(isLike);
            feedbackEntity.get().setUserReviewDetail(review);
            feedbackRepository.save(feedbackEntity.get());
            return true;
        }
        return false;
    }



}
