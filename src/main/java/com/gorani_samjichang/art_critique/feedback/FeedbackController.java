package com.gorani_samjichang.art_critique.feedback;

import com.gorani_samjichang.art_critique.appConstant.FeedbackState;
import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.credit.CreditEntity;
import com.gorani_samjichang.art_critique.credit.CreditRepository;
import com.gorani_samjichang.art_critique.credit.CreditUsedHistoryEntity;
import com.gorani_samjichang.art_critique.credit.CreditUsedHistoryRepository;
import com.gorani_samjichang.art_critique.member.CustomUserDetails;
import com.gorani_samjichang.art_critique.member.MemberEntity;
import com.gorani_samjichang.art_critique.member.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    @Value("${feedback.server.host}")
    private String feedbackServerHost;
    final EmitterService emitterService;

    @GetMapping("/public/good-image")
    List<FeedbackUrlDto> getGoodImage() {
        return feedbackService.getGoodImage();
    }

    @PostMapping("/request")
    ResponseEntity<String> requestFeedback(
            @RequestParam("image") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        return feedbackService.requestFeedback(imageFile, userDetails);
    }

    @GetMapping(value = "/public/retrieve/{serialNumber}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter retrieve(@PathVariable String serialNumber) {
        SseEmitter emitter = new SseEmitter(100 * 1000L);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            try {
                Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
                emitter.send(SseEmitter.event()
                        .name("pending")
                        .data("{\"rate\":" + feedbackEntity.get().getProgressRate() + "}")
                );
                feedbackEntity.get().setProgressRate(feedbackEntity.get().getProgressRate() + 1);
                feedbackRepository.save(feedbackEntity.get());
                if (feedbackEntity.get().getState().equals(FeedbackState.COMPLETED)) {
                    RetrieveFeedbackDto dto = generateRetrieveFeedbackDto(feedbackEntity.get());
                    emitter.send(SseEmitter.event()
                            .name("completed")
                            .data(dto)
                    );
                    emitter.complete();
                    executor.shutdown();
                } else if (feedbackEntity.get().getState().equals(FeedbackState.FAIL) || feedbackEntity.get().getProgressRate() > 100) {
                    // 타임아웃이 왜 안되는지 모르겠어서 여기서 땜빵함
                    feedbackEntity.get().setState(FeedbackState.FAIL);
                    feedbackRepository.save(feedbackEntity.get());

                    RetrieveFeedbackDto dto = generateRetrieveFeedbackDto(feedbackEntity.get());
                    emitter.send(SseEmitter.event()
                            .name("fail")
                            .data(dto)
                    );
                    emitter.complete();
                    executor.shutdown();
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
                executor.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);

        emitter.onCompletion(executor::shutdown);
        emitter.onTimeout(() -> {
            Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
            RetrieveFeedbackDto dto = generateRetrieveFeedbackDto(feedbackEntity.get());
            try {
                emitter.send(SseEmitter.event()
                        .name("fail")
                        .data(dto)
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            emitter.complete();
            executor.shutdown();
        });
        emitter.onError((ex) -> {
            Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
            RetrieveFeedbackDto dto = generateRetrieveFeedbackDto(feedbackEntity.get());
            try {
                emitter.send(SseEmitter.event()
                        .name("fail")
                        .data(dto)
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            executor.shutdown();
        });

        return emitter;
    }

    @GetMapping("/public/simple-retrieve/{serialNumber}")
    RetrieveFeedbackDto simpleRetrieve(
            @PathVariable String serialNumber,
            HttpServletResponse response) {
        Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
        if (feedbackEntity.isPresent()) {
            RetrieveFeedbackDto dto = generateRetrieveFeedbackDto(feedbackEntity.get());
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

    @GetMapping("/bookmark/{page}")
    List<PastFeedbackDto> bookmarkZero(@AuthenticationPrincipal CustomUserDetails userDetail, @PathVariable int page) {
        return feedbackService.getFeedbackBookmark(userDetail.getUid(), page);
    }

    // todo: 0에 대한 처리가 끝나면 지울것.
    @GetMapping("/bookmark")
    List<PastFeedbackDto> bookmark(@AuthenticationPrincipal CustomUserDetails userDetail) {
        return feedbackService.getFeedbackBookmark(userDetail.getUid(), 0);
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
                .tail(oldFeedbackEntity.get().getSerialNumber())
                .build();
        feedbackRepository.save(newFeedbackEntity);
        myMemberEntity.get().addFeedback(newFeedbackEntity);
        memberRepository.save(myMemberEntity.get());

        String jsonData = "{\"image_url\": " + "\"" + imageUrl + "\"}";
        webClientBuilder.build()
                .post()
                .uri(feedbackServerHost + "/request")
                .header("Content-Type", "application/json")
                .bodyValue(jsonData)
                .retrieve()
                .bodyToMono(FeedbackEntity.class)
                .doOnError(error -> {
                    usedCredit.refundCredit();
                    newFeedbackEntity.setState(FeedbackState.FAIL);
                    LocalDateTime NOW = LocalDateTime.now();
                    newFeedbackEntity.setCreatedAt(NOW);
                    creditRepository.save(usedCredit);
                    feedbackRepository.save(newFeedbackEntity);
                    memberRepository.save(myMemberEntity.get());
                })
                .doOnNext(pythonResponse -> {
                    for (FeedbackResultEntity fre : pythonResponse.getFeedbackResults()) {
                        fre.setFeedbackEntity(newFeedbackEntity);
                    }
                    commonUtil.copyNonNullProperties(pythonResponse, newFeedbackEntity);
                    LocalDateTime NOW = LocalDateTime.now();
                    newFeedbackEntity.setCreatedAt(NOW);
                    CreditUsedHistoryEntity historyEntity = CreditUsedHistoryEntity.builder()
                            .type(usedCredit.getType())
                            .usedDate(NOW)
                            .memberEntity(myMemberEntity.get())
                            .feedbackEntity(newFeedbackEntity)
                            .build();
                    newFeedbackEntity.setIsHead(true);
                    oldFeedbackEntity.get().setIsHead(false);
                    creditUsedHistoryRepository.save(historyEntity);

                    feedbackRepository.save(oldFeedbackEntity.get());
                    feedbackRepository.save(newFeedbackEntity);
                })
                .subscribe();

        return new ResponseEntity<>(newSerialNumber, HttpStatusCode.valueOf(200));
    }

    // todo: false 반환이 없음.
    @PostMapping("/turn-off-bookmark/{serialNumber}")
    boolean turnOffBookMark(@PathVariable String serialNumber, @AuthenticationPrincipal CustomUserDetails userDetails) {
        feedbackService.turnBookmark(serialNumber, userDetails.getUid(), false);
        return true;
    }

    // todo: false 반환이 없음.
    @PostMapping("/turn-on-bookmark/{serialNumber}")
    boolean turnOnBookMark(@PathVariable String serialNumber, @AuthenticationPrincipal CustomUserDetails userDetails) {
        feedbackService.turnBookmark(serialNumber, userDetails.getUid(), true);
        return true;
    }

    RetrieveFeedbackDto generateRetrieveFeedbackDto(FeedbackEntity feedbackEntity) {
        RetrieveFeedbackDto dto = RetrieveFeedbackDto.builder()
                .isBookmarked(feedbackEntity.getIsBookmarked())
                .version(feedbackEntity.getVersion())
                .createdAt(feedbackEntity.getCreatedAt())
                .pictureUrl(feedbackEntity.getPictureUrl())
                .serialNumber(feedbackEntity.getSerialNumber())
                .userReviewDetail(feedbackEntity.getUserReviewDetail())
                .userReview(feedbackEntity.getUserReview())
                .state(feedbackEntity.getState())
                .tail(feedbackEntity.getTail())
                .totalScore(feedbackEntity.getTotalScore())
                .memberNickname(feedbackEntity.getMemberEntity().getNickname())
                .memberProfileUrl(feedbackEntity.getMemberEntity().getProfile())
                .memberSerialNumber(feedbackEntity.getMemberEntity().getSerialNumber())
                .build();

        System.out.println(feedbackEntity.getMemberEntity());
        List<FeedbackResultDto> ResultDtoList = new ArrayList<>();
        for (FeedbackResultEntity e : feedbackEntity.getFeedbackResults()) {
            FeedbackResultDto resultDto = new FeedbackResultDto();
            resultDto.setFeedbackContent(e.getFeedbackContent());
            resultDto.setFeedbackType(e.getFeedbackType());
            resultDto.setFeedbackDisplay(e.getFeedbackDisplay());
            ResultDtoList.add(resultDto);
        }
        dto.setFeedbackResults(ResultDtoList);
        return dto;
    }

    @GetMapping("/historyGeneralInfo")
    public ResponseEntity<HashMap<String, Object>> historyGeneralInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        LocalDate currentTime = LocalDate.now();
        List<LocalDateTime> feedbackLog = feedbackRepository.getFeedbackLogByUid(userDetails.getUid());
        Long feedbackAvg = feedbackRepository.getAvgTotalScoreOfFeedbackByUid(userDetails.getUid());

        Map<LocalDate, Long> countByDate = feedbackLog.stream()
                .collect(Collectors.groupingBy(
                        date -> date.toLocalDate(),
                        Collectors.counting()
                ));
        HashMap<String, Object> dto = new HashMap<>();
        dto.put("current", currentTime);
        dto.put("log", countByDate);
        dto.put("count", feedbackLog.size());
        dto.put("avg", feedbackAvg);

        List<LocalDate> streakDates = new ArrayList<>(countByDate.keySet());
        Collections.sort(streakDates);
        if (streakDates.size() > 0) {
            int streakCurrent = -1;
            int maxStreak = 0;
            LocalDate streakStart = null;
            LocalDate streakEnd = null;
            LocalDate previousDate = streakDates.get(0).minusDays(2);
            for (LocalDate date : streakDates) {
                if (previousDate.plusDays(1).equals(date)) {
                    streakCurrent++;
                } else {
                    if (streakCurrent >= maxStreak) {
                        maxStreak = streakCurrent;
                        streakEnd = date;
                        streakStart = date.minusDays(maxStreak - 1);
                    }
                    streakCurrent = 1;
                }
                previousDate = date;
            }
            LocalDate date = streakDates.get(streakDates.size() - 1);
            if (streakCurrent >= maxStreak) {
                maxStreak = streakCurrent;
                streakEnd = date;
                streakStart = date.minusDays(maxStreak - 1);
            }
            if (!(previousDate.equals(LocalDate.now().minusDays(1)) | previousDate.equals(LocalDate.now()))) {
                streakCurrent = 0;
            }
            dto.put("streakCurrent", streakCurrent);
            dto.put("maxStreak", maxStreak);
            dto.put("streakStart", streakStart);
            dto.put("streakEnd", streakEnd);
        } else {
            dto.put("streakCurrent", 0);
            dto.put("maxStreak", 0);
            dto.put("streakStart", null);
            dto.put("streakEnd", null);
        }
        return new ResponseEntity(dto, HttpStatusCode.valueOf(200));
    }

    @GetMapping("/public/allFeedbackedImage/{serialNumber}")
    public ResponseEntity<List<FeedbackUrlDto>> allFeedbackImage(@PathVariable String serialNumber) {
        List<FeedbackUrlDto> dto = feedbackRepository.findAllserialNumberAndPictureUrlByMemberEntityUid(serialNumber);
        return new ResponseEntity<>(dto, HttpStatusCode.valueOf(200));
    }
}