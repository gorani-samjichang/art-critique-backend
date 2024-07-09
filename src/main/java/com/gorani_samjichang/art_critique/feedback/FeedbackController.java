package com.gorani_samjichang.art_critique.feedback;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {
    String [] todayGoodImage = new String [] {
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
    @GetMapping("/goodImage")
    String [] getGoodImage() {
//        ArrayList<String> data = new ArrayList<>();
//        for (int i = 0; i < 15; i++)
//            data.add(String.format("https://picsum.photos/id/" + (int)(Math.floor(Math.random() * (100 + 1))) + "/" + (int)((Math.floor(Math.random() * (10 + 1)) + 5)*20) + "/" + (int)((Math.floor(Math.random() * (10 + 1)) + 5)*20)));
        return todayGoodImage;
    }

    @Async("taskExecutor")
    @PostMapping("/requestFeedback")
    CompletableFuture<String> requestFeedback(@RequestParam("image") MultipartFile imageFile) throws IOException {
        try {
            // 3초(3000밀리초) 동안 대기
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        String dummyFeedback = "[{\"type\": \"점수\",\"content\": {\"item\": [{\"item_name\": \"인체이해도\",\"item_grade\": 55},{\"item_name\": \"구도와 표현력\",\"item_grade\": 38},{\"item_name\": \"명암과 그림자\",\"item_grade\": 26},{\"item_name\": \"비례와 원근\",\"item_grade\": 52},{\"item_name\": \"색체 이해도\",\"item_grade\": 83}]}},{\"type\": \"종합평가\",\"content\": {\"class\": \"중수\",\"detail\": \"이 그림은 전체적으로 높은 수준의 기술과 창의성을 보여주지만, 일부 세부적인 부분에서 개선할 수 있는 여지가 있습니다. 비율과 해부학적 정확성을 조금 더 현실감 있게 조절하고, 동작과 자세를 더욱 역동적으로 표현하며, 디테일과 정확성을 더 높이고, 조명과 그림자의 사용을 더욱 정교하게 하며, 색채와 채색의 깊이를 더욱 다채롭게 만들면 그림의 완성도가 한층 더 높아질 것입니다.\"}}]";
        return CompletableFuture.completedFuture(dummyFeedback);

    }
}
