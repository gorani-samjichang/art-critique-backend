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
    final FeedbackService feedbackService;


    @GetMapping("/public/good-image")
    String[] getGoodImage() {
        return feedbackService.getGoodImage();
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
        pythonResponse.setTail(null);
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
    RetrieveFeedbackDto retrieveFeedback(@PathVariable("serialNumber") String serialNumber, HttpServletResponse response) {
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
            response.setStatus(402);
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

    @GetMapping("/recent-order")
    List<PastFeedbackDto> recentOrder(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "0") int page) {
        String email = request.getAttribute("email").toString();
        return feedbackService.getFeedbackRecentOrder(email, page);
    }

    @GetMapping("/score-order")
    List<PastFeedbackDto> scoreOrder(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "0") int page) {
        String email = request.getAttribute("email").toString();
        return feedbackService.getFeedbackTotalScoreOrder(email, page);
    }

    @PostMapping("/remake/{serialNumber}")
    String remake(@RequestParam("image") MultipartFile imageFile,
                  @PathVariable String serialNumber,
                  HttpServletRequest request,
                  HttpServletResponse response) throws IOException {
        System.out.println(imageFile + "  이미지에 대한 재요청");
        Optional<FeedbackEntity> original = feedbackRepository.findBySerialNumber(serialNumber);
        if (original.isEmpty()) {
            response.setStatus(402);
            return null;
        }
        MemberEntity me = memberRepository.findByEmail(request.getAttribute("email").toString());
        me.removeFeedback(original.get());

        String jsonData = "{ \"name\": \"default\" }";

        FeedbackEntity pythonResponse = webClientBuilder.build()
                .post()
                .uri(feedbackServerHost + "/request")
                .header("Content-Type", "application/json")
                .bodyValue(jsonData)
                .retrieve()
                .bodyToMono(FeedbackEntity.class)
                .block();

        assert pythonResponse != null;
        pythonResponse.setCreatedAt(LocalDateTime.now());
        pythonResponse.setIsPublic(original.get().getIsPublic());
        pythonResponse.setIsBookmarked(original.get().getIsBookmarked());
        pythonResponse.setTail(original.get().getFid());
        pythonResponse.setPictureUrl(original.get().getPictureUrl());

        String sn = "a" + bCryptPasswordEncoder.encode(String.valueOf(pythonResponse.getFid()));
        sn = sn.replace("$", "-");
        sn = sn.replace("/", "_");
        sn = sn.replace(".", "Z");
        pythonResponse.setSerialNumber(sn);

        feedbackRepository.save(pythonResponse);
        me.addFeedback(pythonResponse);
        memberRepository.save(me);

        return "null";
    }
}
