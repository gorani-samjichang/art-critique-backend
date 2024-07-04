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
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        String dummyFeedback = "[{\"feedback_type\": \"종합 평가\",\"feedback_content\": {\"feedback_title\": \"종합 평가\",\"class\": \"중수\",\"detail\": \"이 그림은 전체적으로 높은 수준의 기술과 창의성을 보여주지만, 일부 세부적인 부분에서 개선할 수 있는 여지가 있습니다. 비율과 해부학적 정확성을 조금 더 현실감 있게 조절하고, 동작과 자세를 더욱 역동적으로 표현하며, 디테일과 정확성을 더 높이고, 조명과 그림자의 사용을 더욱 정교하게 하며, 색채와 채색의 깊이를 더욱 다채롭게 만들면 그림의 완성도가 한층 더 높아질 것입니다.\"}},{\t\t\"feedback_type\": \"평가 항목1\",\t\t\"feedback_content\": {\"feedback_title\": \"인체 이해도\",\"grade\": 53,\"content\": [{\"title\": \"머리와 몸통 비율\",\"detail\": \"애니메이션 스타일로 그려진 캐릭터라 비율이 다소 과장되어 있습니다. 현실적인 인체 비율을 중시한다면 머리가 너무 커 보일 수 있습니다.\"},{\"title\": \"팔과 다리 비율\",\"detail\": \"다리 길이는 적절하지만, 팔이 조금 짧아 보일 수 있습니다. 팔의 길이가 조금 더 길면 더욱 자연스러울 수 있습니다.\"},{\"title\": \"해부학적 디테일\",\"detail\": \"어깨와 팔의 근육 표현이 다소 부드럽게 표현되어 현실적인 근육의 윤곽이 덜 드러날 수 있습니다. 해부학적 정확성을 높이기 위해 근육의 윤곽을 더 분명하게 표현하는 것이 좋습니다.\"},{\"title\": \"손과 발의 크기와 형태\",\"detail\": \"손가락의 크기와 위치는 잘 표현되었으나, 손의 전체적인 크기가 조금 작게 보일 수 있습니다. 손의 크기를 조금 더 키우면 비율이 더 자연스러울 수 있습니다.\"}],\"recomment_reference\": [\"https://picsum.photos/id/88/180/160\",\"https://picsum.photos/id/50/260/240\",\"https://picsum.photos/id/55/260/280\",\"https://picsum.photos/id/99/100/100\",\"https://picsum.photos/id/69/160/160\"]}},{\t\t\"feedback_type\": \"평가 항목1\",\t\t\"feedback_content\": {\"feedback_title\": \"동작과 자세\",\"grade\": 76,\"content\": [{\"title\": \"포즈의 자연스러움\",\"detail\": \"포즈는 동적이지만, 다소 과장된 느낌이 있습니다. 현실적인 치어리더의 동작을 참고하면 더욱 자연스러운 포즈가 될 수 있습니다.\"},{\"title\": \"몸의 균형\",\"detail\": \"몸의 중심이 잘 잡혀 있지만, 다리의 위치가 조금 더 다양하게 표현되면 동작의 역동성이 더해질 수 있습니다.\"},{\"title\": \"팔과 다리의 움직임\",\"detail\": \"팔과 다리의 움직임이 자연스럽지만, 팔의 각도가 조금 더 역동적이면 좋겠습니다. 팔을 조금 더 높이 들어 올리거나, 손의 위치를 조정하면 더욱 생동감이 느껴질 수 있습니다.\"},{\"title\": \"전체적인 자세\",\"detail\": \"전체적인 자세가 자연스럽지만, 약간의 비틀림이나 자세의 변화를 주어 더욱 흥미로운 포즈를 만들 수 있습니다.\"}],\"recomment_reference\": [\"https://picsum.photos/id/88/180/160\",\"https://picsum.photos/id/50/260/240\",\"https://picsum.photos/id/55/260/280\",\"https://picsum.photos/id/99/100/100\",\"https://picsum.photos/id/69/160/160\"]}}]";
        return CompletableFuture.completedFuture(dummyFeedback);

    }
}
