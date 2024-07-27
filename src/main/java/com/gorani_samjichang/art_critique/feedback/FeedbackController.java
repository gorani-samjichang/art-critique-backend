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
    String requestFeedback(
            @RequestParam("image") MultipartFile imageFile,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        MemberEntity me = memberRepository.findByEmail(String.valueOf(request.getAttribute("email")));
        if (me.getCredit() <= 0) {
            return "noCredit";
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

        me.addFeedback(feedbackEntity);
        me.setCredit(me.getCredit() - 1);

        memberRepository.save(me);

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

                        feedbackEntity.setCreatedAt(LocalDateTime.now());
                        feedbackRepository.save(feedbackEntity);
                    })
                    .subscribe();

        } catch(Exception e) {
            response.setStatus(501);
        }

        return serialNumber;
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
    List<PastFeedbackDto> writeOrder(HttpServletRequest request) {
        MemberEntity me = memberRepository.findByEmail(request.getAttribute("email").toString());
        ArrayList<PastFeedbackDto> feedbackEntities = new ArrayList<>();
        for (FeedbackEntity f : me.getFeedbacks()) {
            if (!f.getIsHead()) continue;
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

        feedbackEntities.sort(Comparator.comparing(PastFeedbackDto::getCreatedAt)); //만들어진 순서로 오름차순

        return feedbackEntities;
    }

    @GetMapping("/recent-order")
    List<PastFeedbackDto> recentOrder(HttpServletRequest request, HttpServletResponse response) {
        MemberEntity me = memberRepository.findByEmail(request.getAttribute("email").toString());
        ArrayList<PastFeedbackDto> feedbackEntities = new ArrayList<>();
        for (FeedbackEntity f : me.getFeedbacks()) {
            if (!f.getIsHead()) continue;
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

        feedbackEntities.sort((o1, o2) -> {
            return o2.getCreatedAt().compareTo(o1.getCreatedAt()); // 만들어진 순서대로 내림차순
        });

        return feedbackEntities;
    }

    @GetMapping("/score-order")
    List<PastFeedbackDto> scoreOrder(HttpServletRequest request) {
        MemberEntity me = memberRepository.findByEmail(request.getAttribute("email").toString());
        ArrayList<PastFeedbackDto> feedbackEntities = new ArrayList<>();
        for (FeedbackEntity f : me.getFeedbacks()) {
            if (!f.getIsHead()) continue;
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

        feedbackEntities.sort((o1, o2) -> {
            return o2.getTotalScore().compareTo(o1.getTotalScore()); // 만들어진 순서대로 내림차순
        });

        return feedbackEntities;
    }

    @GetMapping("/bookmark")
    List<PastFeedbackDto> bookmark(HttpServletRequest request) {
        MemberEntity me = memberRepository.findByEmail(request.getAttribute("email").toString());
        ArrayList<PastFeedbackDto> feedbackDtoList = new ArrayList<>();
        for (FeedbackEntity f : me.getFeedbacks()) {
            if (!f.getIsHead()) continue;
            feedbackDtoList.add(PastFeedbackDto
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

        return feedbackDtoList;
    }

    @PostMapping("/re-request/{serialNumber}")
    String reRequest(@PathVariable String serialNumber,
                  HttpServletRequest request,
                  HttpServletResponse response) {

        MemberEntity myMemberEntity = memberRepository.findByEmail(String.valueOf(request.getAttribute("email")));
        if (myMemberEntity.getCredit() <= 0) {
            response.setStatus(401);
            return null;
        }

        myMemberEntity.setCredit(myMemberEntity.getCredit() - 1);
        memberRepository.save(myMemberEntity);

        Optional<FeedbackEntity> oldFeedbackEntity = feedbackRepository.findBySerialNumber(serialNumber);

        String newSerialNumber = UUID.randomUUID().toString();
        System.out.println(serialNumber);
        System.out.println(newSerialNumber);
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

                        newFeedbackEntity.setCreatedAt(LocalDateTime.now());
                        newFeedbackEntity.setIsHead(true);
                        oldFeedbackEntity.get().setIsHead(false);
                        myMemberEntity.addFeedback(newFeedbackEntity);

                        feedbackRepository.save(newFeedbackEntity);
                        memberRepository.save(myMemberEntity);
                    })
                    .subscribe();
        } catch(Exception e) {
            response.setStatus(501);
        }

        return newSerialNumber;
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
}
