package com.gorani_samjichang.art_critique.feedback;

import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    final FeedbackRepository feedbackRepository;
    final MemberRepository memberRepository;
    final CommonUtil commonUtil;
    final BCryptPasswordEncoder bCryptPasswordEncoder;
    final WebClient.Builder webClientBuilder;
    @Value("${feedback.server.host}")
    private String feedbackServerHost;

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
    String requestFeedback(@RequestParam("image") MultipartFile imageFile, HttpServletRequest request) throws IOException {

        String jsonData = "{ \"name\": \"default\" }";

        FeedbackEntity pythonResponse = webClientBuilder.build()
                .post()
                .uri(feedbackServerHost + "/request")
                .header("Content-Type", "application/json")
                .bodyValue(jsonData)
                .retrieve()
                .bodyToMono(FeedbackEntity.class)
                .block();

        pythonResponse.setCreatedAt(LocalDateTime.now());
        pythonResponse.setIsPublic(true);
        pythonResponse.setIsBookmarked(false);
        feedbackRepository.save(pythonResponse);

        String serialNumber = "a" + bCryptPasswordEncoder.encode(String.valueOf(pythonResponse.getFid()));
        serialNumber = serialNumber.replace("$", "-");
        serialNumber = serialNumber.replace("/", "_");
        serialNumber = serialNumber.replace(".", "Z");
        pythonResponse.setSerialNumber(serialNumber);
        String imageUrl = commonUtil.uploadToStorage(imageFile, serialNumber);
        pythonResponse.setPictureUrl(imageUrl);
        feedbackRepository.save(pythonResponse);


        MemberEntity me = memberRepository.findByEmail(String.valueOf(request.getAttribute("email")));
        me.addFeedback(pythonResponse);

        memberRepository.save(me);

        return pythonResponse.getSerialNumber();
    }

    @GetMapping("/public/retrieve/{serialNumber}")
    RetrieveFeedbackDto retrieveFeedback(@PathVariable("serialNumber") String serialNumber) {
        Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
        if (feedbackEntity.isPresent()) {
            RetrieveFeedbackDto dto = RetrieveFeedbackDto.builder()
                    .isBookmarked(feedbackEntity.get().getIsBookmarked())
                    .version(feedbackEntity.get().getVersion())
                    .createdAt(feedbackEntity.get().getCreatedAt())
                    .pictureUrl(feedbackEntity.get().getPictureUrl())
                    .serialNumber(feedbackEntity.get().getSerialNumber())
                    .userReviewDetail(feedbackEntity.get().getUserReviewDetail())
                    .userReview(feedbackEntity.get().getUserReview())
                    .build();

            List<FeedbackResultDto> ResultDtoList = new ArrayList<>();
            for (FeedbackResultEntity e : feedbackEntity.get().getFeedbackResults()) {
                FeedbackResultDto resultDto = new FeedbackResultDto();
                resultDto.setFeedbackContent(e.getFeedbackContent());
                resultDto.setFeedbackType(e.getFeedbackType());
                resultDto.setFeedbackDisplay(e.getFeedbackDisplay());
                ResultDtoList.add(resultDto);
            }
            dto.setFeedbackResults(ResultDtoList);
            return dto;
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

    public static String decodeUnicodeEscapes(String unicodeStr) {
        StringBuilder sb = new StringBuilder();
        char[] chars = unicodeStr.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '\\' && chars[i + 1] == 'u') {
                String hex = unicodeStr.substring(i + 2, i + 6);
                sb.append((char) Integer.parseInt(hex, 16));
                i += 5;
            } else {
                sb.append(chars[i]);
            }
        }
        return sb.toString();
    }

    @GetMapping("/recent-order")
    List<PastFeedbackDto> recentOrder(HttpServletRequest request) {
        MemberEntity me = memberRepository.findByEmail(request.getAttribute("email").toString());
        ArrayList<PastFeedbackDto> feedbackEntities = new ArrayList<>();
        for (FeedbackEntity f : me.getFeedbacks()) {
            feedbackEntities.add(PastFeedbackDto
                    .builder()
                    .isBookmarked(f.getIsBookmarked())
                    .pictureUrl(f.getPictureUrl())
                    .createdAt(f.getCreatedAt())
                    .version(f.getVersion())
                    .serialNumber(f.getSerialNumber())
                    .isSelected(false)
                    .build());
        }
        feedbackEntities.sort((o1, o2) -> {
            return o2.getCreatedAt().compareTo(o1.getCreatedAt());
        });

        return feedbackEntities;
    }


}