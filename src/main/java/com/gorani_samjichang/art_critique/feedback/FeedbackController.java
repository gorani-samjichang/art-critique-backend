package com.gorani_samjichang.art_critique.feedback;

import com.gorani_samjichang.art_critique.appConstant.FeedbackState;
import com.gorani_samjichang.art_critique.common.CommonUtil;
import com.gorani_samjichang.art_critique.common.S3Utils;
import com.gorani_samjichang.art_critique.common.exceptions.NoPermissionException;
import com.gorani_samjichang.art_critique.common.exceptions.RegenerationFeedbackNotFoundException;
import com.gorani_samjichang.art_critique.common.exceptions.UserNotFoundException;
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
import org.springframework.context.annotation.DependsOn;
import lombok.extern.slf4j.Slf4j;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
@DependsOn("s3Utils")
public class FeedbackController {
    private final S3Utils s3Utils;
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
        SseEmitter emitter = new SseEmitter(200 * 1000L);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();


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

        executor.scheduleAtFixedRate(() -> {
            try {
                Optional<FeedbackEntity> feedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);
                
                if (feedbackEntity.get().getState().equals(FeedbackState.PENDING) || feedbackEntity.get().getState().equals(FeedbackState.NOT_STARTED)) {
                    emitter.send(SseEmitter.event()
                            .name("pending")
                            .data("{\"rate\":" + feedbackEntity.get().getProgressRate() + "}")
                    );
                } else if (feedbackEntity.get().getState().equals(FeedbackState.COMPLETED)) {
                    RetrieveFeedbackDto dto = generateRetrieveFeedbackDto(feedbackEntity.get());
                    emitter.send(SseEmitter.event()
                            .name("completed")
                            .data(dto)
                    );
                    emitter.complete();
                    executor.shutdown();
                } else if (feedbackEntity.get().getState().equals(FeedbackState.FAIL)) {
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
                                     @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException, UserNotFoundException, RegenerationFeedbackNotFoundException {
        return feedbackService.requestFeedback(serialNumber, userDetails);
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

        // presignedUrl 생성 메서드 만들어서

        RetrieveFeedbackDto dto = RetrieveFeedbackDto.builder()
                .isBookmarked(feedbackEntity.getIsBookmarked())
                .version(feedbackEntity.getVersion())
                .createdAt(feedbackEntity.getCreatedAt())
                .pictureUrl(feedbackEntity.getPictureUrl())
                // .Image3DUrl(어쩌고);
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

        List<FeedbackResultDto> ResultDtoList = new ArrayList<>();
        for (FeedbackResultEntity e : feedbackEntity.getFeedbackResults()) {
            FeedbackResultDto resultDto = new FeedbackResultDto();
            resultDto.setFeedbackContent(e.getFeedbackContent());
            resultDto.setFeedbackType(e.getFeedbackType());
            resultDto.setFeedbackDisplay(e.getFeedbackDisplay());
            ResultDtoList.add(resultDto);

            try {
                if ("3d-model".equals(e.getFeedbackType())) {
                    JsonObject jsonObject = JsonParser.parseString(e.getFeedbackContent()).getAsJsonObject();
                    if (!jsonObject.has("s3_url")) {
                        continue;
                    }
                    String s3UrlString = jsonObject.get("s3_url").getAsString();
                    URI s3Uri = new URI(s3UrlString);

                    String bucket = s3Uri.getHost();
                    String key = s3Uri.getPath().substring(1);

                    String presigned_url = s3Utils.getPresignedUrl(bucket, key);

                    JsonObject newFeedbackContentJson = new JsonObject();
                    newFeedbackContentJson.addProperty("image_url", presigned_url);
                    String newFeedbackContent = newFeedbackContentJson.toString();
                    resultDto.setFeedbackContent(newFeedbackContent);
                }
            } catch(Exception exception) {
                log.error("Failed to PresignedGetObjectRequest. e.getFrid=" + e.getFrid(), exception);
            }
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

    @GetMapping("/public/thread/{serialNumber}")
    public List<CommentDto> getCommentInfos(@PathVariable String serialNumber) {
        return feedbackService.findCommentBySerialNumber(serialNumber);
    }

    @PostMapping("/thread/{serialNumber}")
    public void addComment(@PathVariable String serialNumber, @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam String content) {
        feedbackService.addComment(serialNumber, userDetails, content);
    }
}