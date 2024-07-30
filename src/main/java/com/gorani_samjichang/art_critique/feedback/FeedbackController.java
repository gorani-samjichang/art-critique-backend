package com.gorani_samjichang.art_critique.feedback;

import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.credit.CreditEntity;
import com.gorani_samjichang.art_critique.credit.CreditRepository;
import com.gorani_samjichang.art_critique.credit.CreditUsedHistoryEntity;
import com.gorani_samjichang.art_critique.credit.CreditUsedHistoryRepository;
import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

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
    ResponseEntity<String> requestFeedback(
            @RequestParam("image") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails, HttpServletResponse response) throws IOException {

        Optional<MemberEntity> me = memberRepository.findById(userDetails.getUid());
        CreditEntity usedCredit = creditRepository.usedCreditEntityByRequest(userDetails.getUid());
        if (me.isEmpty()) return new ResponseEntity<>(null, HttpStatusCode.valueOf(401));
        if (me.get().getCredit() <= 0 || usedCredit == null) {
            return new ResponseEntity<>("noCredit", HttpStatusCode.valueOf(200));
        }

        String serialNumber = UUID.randomUUID().toString();
        String imageUrl = commonUtil.uploadToStorage(imageFile, serialNumber);
        FeedbackEntity feedbackEntity = FeedbackEntity
                .builder()
                .serialNumber(serialNumber)
                .state("NOT_STARTED")
                .progressRate(0)
                .isBookmarked(false)
                .isPublic(true)
                .pictureUrl(imageUrl)
                .isHead(true)
                .tail(null)
                .build();

        feedbackRepository.save(feedbackEntity);

        me.get().addFeedback(feedbackEntity);
        me.get().setCredit(me.get().getCredit() - 1);
        usedCredit.useCredit();

        creditRepository.save(usedCredit);

        memberRepository.save(me.get());

        try {
            String jsonData = "{\"name\": " + "\"" + "imageUrl" + "\"}";
            webClientBuilder.build()
                    .post()
                    .uri(feedbackServerHost + "/request")
                    .header("Content-Type", "application/json")
                    .bodyValue(jsonData)
                    .retrieve()
                    .bodyToMono(FeedbackEntity.class)
                    .doOnNext(pythonResponse -> {
                        if (pythonResponse != null) {
                            commonUtil.copyNonNullProperties(pythonResponse, feedbackEntity);
                            for (FeedbackResultEntity fre : feedbackEntity.getFeedbackResults()) {
                                fre.setFeedbackEntity(feedbackEntity);
                            }
                        }

                        LocalDateTime NOW = LocalDateTime.now();
                        feedbackEntity.setCreatedAt(NOW);
                        CreditUsedHistoryEntity historyEntity = CreditUsedHistoryEntity.builder()
                                .type(usedCredit.getType())
                                .usedDate(NOW)
                                .feedbackEntity(feedbackEntity)
                                .build();
                        me.get().addCreditHistory(historyEntity);
                        creditUsedHistoryRepository.save(historyEntity);
                        feedbackRepository.save(feedbackEntity);
                    })
                    .subscribe();

        } catch(Exception e) {
            return new ResponseEntity<>(null, HttpStatusCode.valueOf(501));
        }

        return new ResponseEntity<>(serialNumber, HttpStatusCode.valueOf(200));
    }

    @GetMapping("/public/retrieve/{serialNumber}")
    public SseEmitter retrieve(@PathVariable String serialNumber, HttpServletResponse response) {
        SseEmitter emitter = new SseEmitter();
        String imageUrl = feedbackRepository.getImageUrlBySerialNumber(serialNumber);

        // threadSafe 한지 아직 알 수 없음
        // 아마 이 원리 그대로 진행 할 것 같음
        // 프론트가 접속이 끊겨도 내부적으로 데이터베이스에 파이썬의 입력값이 들어와야함 따라서
        // 프론트 - 스프링 / 스프링 - 파이썬 은 완전히 별개의 연결이고 파이썬과 스프링의 연결의 결과는 데이터베이스에 반영이되야하고, 스프링과 프론트의 연결은 프론트에 반영되야함
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (int i = 0; i <= 120; i++) {
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

    @GetMapping("/public/simple-retrieve/{serialNumber}")
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

    @GetMapping("/write-order")
    List<PastFeedbackDto> writeOrder(@AuthenticationPrincipal CustomUserDetails userDetail) {
        List<FeedbackEntity> entities = feedbackRepository.findByWriteOrder(userDetail.getUid());
        return convertFeedbackEntityToDto(entities);
    }

    @GetMapping("/recent-order")
    List<PastFeedbackDto> recentOrder(@AuthenticationPrincipal CustomUserDetails userDetail) {
        List<FeedbackEntity> entities = feedbackRepository.findByRecentOrder(userDetail.getUid());
        return convertFeedbackEntityToDto(entities);
    }

    @GetMapping("/score-order")
    List<PastFeedbackDto> scoreOrder(@AuthenticationPrincipal CustomUserDetails userDetail) {
        List<FeedbackEntity> entities = feedbackRepository.findByScoreOrder(userDetail.getUid());
        return convertFeedbackEntityToDto(entities);
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
        } catch(Exception e) {
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
