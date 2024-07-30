package com.gorani_samjichang.art_critique.feedback;

import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.credit.CreditEntity;
import com.gorani_samjichang.art_critique.credit.CreditRepository;
import com.gorani_samjichang.art_critique.credit.CreditUsedHistoryRepository;
import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    final FeedbackService feedbackService;
    final FeedbackRepository feedbackRepository;
    final MemberRepository memberRepository;
    final CreditRepository creditRepository;
    final CreditUsedHistoryRepository creditUsedHistoryRepository;
    final CommonUtil commonUtil;
    final BCryptPasswordEncoder bCryptPasswordEncoder;
    final WebClient.Builder webClientBuilder;
    @Value("${feedback.server.host}")
    private String feedbackServerHost;
    final EmitterService emitterService;


    @GetMapping("/public/good-image")
    String[] getGoodImage() {
        return feedbackService.getGoodImage();
    }

    @PostMapping("/request")
    ResponseEntity<String> requestFeedback(
            @RequestParam("image") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        return new ResponseEntity<>(feedbackService.requestFeedback(imageFile, userDetails), HttpStatusCode.valueOf(200));
    }

    @GetMapping("/public/retrieve/{serialNumber}")
    public SseEmitter retrieve(@PathVariable String serialNumber, HttpServletResponse response) {
        SseEmitter emitter = new SseEmitter();

        // threadSafe 한지 아직 알 수 없음
        // 아마 이 원리 그대로 진행 할 것 같음
        // 프론트가 접속이 끊겨도 내부적으로 데이터베이스에 파이썬의 입력값이 들어와야함 따라서
        // 프론트 - 스프링 / 스프링 - 파이썬 은 완전히 별개의 연결이고 파이썬과 스프링의 연결의 결과는 데이터베이스에 반영이되야하고, 스프링과 프론트의 연결은 프론트에 반영되야함
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (int i = 0; i <= 50; i++) {
                    Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
                    emitter.send(SseEmitter.event()
                                    .name("pending")
                                    .data("{\"rate\":" + feedbackEntity.get().getProgressRate() + "}")
//                            .data("{\"rate\":" + i + "}")
                    );
                    TimeUnit.MILLISECONDS.sleep(1000);
                    if (feedbackEntity.get().getState().equals("COMPLETED")) {
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
                        emitter.send(SseEmitter.event()
                                .name("completed")
                                .data(dto)
                        );
                        break;
                    }
                }
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                System.out.println("!!!");
            }
        });
        return emitter;
//        return emitterService.connection(serialNumber, response);
    }

    @GetMapping("/retrieve/{serialNumber}")
    RetrieveFeedbackDto simpleRetrieve(
            @PathVariable String serialNumber,
            HttpServletResponse response) {
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
                    .state(feedbackEntity.get().getState())
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
            response.setStatus(402);
            return null;
        }
    }

    @PostMapping("review/{serialNumber}")
    boolean feedbackReview(@PathVariable String serialNumber,
                           @RequestParam boolean isLike,
                           @RequestParam String review,
                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return feedbackService.feedbackReview(serialNumber, isLike, review, userDetails);
    }

    @GetMapping("/write-order/{page}")
    List<PastFeedbackDto> writeOrder(@AuthenticationPrincipal CustomUserDetails userDetail, @PathVariable int page) {
        return feedbackService.getFeedbackCreatedAtOrder(userDetail.getUid(), page);
    }

    @GetMapping("/recent-order/{page}")
    List<PastFeedbackDto> recentOrder(@AuthenticationPrincipal CustomUserDetails userDetail, @PathVariable int page) {
        return feedbackService.getFeedbackRecentOrder(userDetail.getUid(), page);
    }

    @GetMapping("/score-order/{page}")
    List<PastFeedbackDto> scoreOrder(@AuthenticationPrincipal CustomUserDetails userDetail, @PathVariable int page) {
        return feedbackService.getFeedbackTotalScoreOrder(userDetail.getUid(), page);
    }

    // todo: 0에 대한 처리가 끝나면 지울것.
    @GetMapping("/write-order")
    List<PastFeedbackDto> writeOrderZero(@AuthenticationPrincipal CustomUserDetails userDetail) {
        return feedbackService.getFeedbackCreatedAtOrder(userDetail.getUid(), 0);
    }

    // todo: 0에 대한 처리가 끝나면 지울것.
    @GetMapping("/recent-order")
    List<PastFeedbackDto> recentOrderZero(@AuthenticationPrincipal CustomUserDetails userDetail) {
        return feedbackService.getFeedbackRecentOrder(userDetail.getUid(), 0);
    }

    // todo: 0에 대한 처리가 끝나면 지울것.
    @GetMapping("/score-order")
    List<PastFeedbackDto> scoreOrderZero(@AuthenticationPrincipal CustomUserDetails userDetail) {
        return feedbackService.getFeedbackTotalScoreOrder(userDetail.getUid(), 0);
    }

    @GetMapping("/bookmark")
    List<PastFeedbackDto> bookmark(@AuthenticationPrincipal CustomUserDetails userDetail) {
        List<FeedbackEntity> entities = feedbackRepository.findByUidAndBookmarked(userDetail.getUid());
        return convertFeedbackEntityToDto(entities);
    }

    @PostMapping("/re-request/{serialNumber}")
    ResponseEntity<String> reRequest(@PathVariable String serialNumber,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     HttpServletResponse response) {

        Optional<MemberEntity> myMemberEntity = memberRepository.findById(userDetails.getUid());
        CreditEntity usedCredit = creditRepository.usedCreditEntityByRequest(userDetails.getUid());
        if (myMemberEntity.isEmpty()) return new ResponseEntity<>(null, HttpStatusCode.valueOf(401));
        if (myMemberEntity.get().getCredit() <= 0 || usedCredit == null) {
            return new ResponseEntity<>("noCredit", HttpStatusCode.valueOf(200));
        }

        myMemberEntity.get().setCredit(myMemberEntity.get().getCredit() - 1);
        usedCredit.useCredit();
        creditRepository.save(usedCredit);

        Optional<FeedbackEntity> oldFeedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);

        String newSerialNumber = UUID.randomUUID().toString();
        String imageUrl = oldFeedbackEntity.get().getPictureUrl();
        FeedbackEntity newFeedbackEntity = FeedbackEntity
                .builder()
                .serialNumber(newSerialNumber)
                .state("NOT_STARTED")
                .progressRate(0)
                .pictureUrl(imageUrl)
                .isPublic(oldFeedbackEntity.get().getIsPublic())
                .isBookmarked(oldFeedbackEntity.get().getIsBookmarked())
                .tail(oldFeedbackEntity.get().getFid())
                .build();
        feedbackRepository.save(newFeedbackEntity);
        myMemberEntity.get().addFeedback(newFeedbackEntity);
        memberRepository.save(myMemberEntity.get());

        try {
            String jsonData = "{\"name\": " + "\"" + imageUrl + "\"}";
            webClientBuilder.build()
                    .post()
                    .uri(feedbackServerHost + "/request")
                    .header("Content-Type", "application/json")
                    .bodyValue(jsonData)
                    .retrieve()
                    .bodyToMono(FeedbackEntity.class)
                    .doOnNext(pythonResponse -> {
                        if (pythonResponse != null) {
                            commonUtil.copyNonNullProperties(pythonResponse, newFeedbackEntity);
                            for (FeedbackResultEntity fre : newFeedbackEntity.getFeedbackResults()) {
                                fre.setFeedbackEntity(newFeedbackEntity);
                            }
                        }
                        LocalDateTime NOW = LocalDateTime.now();
                        newFeedbackEntity.setCreatedAt(NOW);
                        CreditUsedHistoryEntity historyEntity = CreditUsedHistoryEntity.builder()
                                .type(usedCredit.getType())
                                .usedDate(NOW)
                                .feedbackEntity(newFeedbackEntity)
                                .build();
                        myMemberEntity.get().addCreditHistory(historyEntity);
                        newFeedbackEntity.setIsHead(true);
                        oldFeedbackEntity.get().setIsHead(false);

                        feedbackRepository.save(oldFeedbackEntity.get());
                        feedbackRepository.save(newFeedbackEntity);
                    })
                    .subscribe();
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatusCode.valueOf(501));
        }

        return new ResponseEntity<>(newSerialNumber, HttpStatusCode.valueOf(200));
    }

    @PostMapping("/turn-off-bookmark/{serialNumber}")
    boolean turnOffBookMark(@PathVariable String serialNumber, HttpServletResponse response) {
        Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
        if (feedbackEntity.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        if (!feedbackEntity.get().getIsBookmarked()) {
            return false;
        }
        feedbackEntity.get().setIsBookmarked(false);
        feedbackRepository.save(feedbackEntity.get());
        return true;
    }

    @PostMapping("/turn-on-bookmark/{serialNumber}")
    boolean turnOnBookMark(@PathVariable String serialNumber, HttpServletResponse response) {
        Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
        if (feedbackEntity.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        if (feedbackEntity.get().getIsBookmarked()) {
            return false;
        }
        feedbackEntity.get().setIsBookmarked(true);
        feedbackRepository.save(feedbackEntity.get());
        return true;
    }

    List<PastFeedbackDto> convertFeedbackEntityToDto(List<FeedbackEntity> list) {
        ArrayList<PastFeedbackDto> feedbackEntities = new ArrayList<>();
        for (FeedbackEntity f : list) {
            feedbackEntities.add(PastFeedbackDto
                    .builder()
                    .isBookmarked(f.getIsBookmarked())
                    .pictureUrl(f.getPictureUrl())
                    .createdAt(f.getCreatedAt())
                    .version(f.getVersion())
                    .serialNumber(f.getSerialNumber())
                    .totalScore(f.getTotalScore())
                    .isSelected(false)
                    .build());
        }
        return feedbackEntities;
    }
}