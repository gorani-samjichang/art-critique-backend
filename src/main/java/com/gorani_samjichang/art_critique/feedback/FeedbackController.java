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
        String dummyFeedback = "[{\"feedback_type\":\"종합평가\",\"feedback_content\":{\"class\":\"중수\",\"detail\":\"이그림은전체적으로높은수준의기술과창의성을보여주지만,일부세부적인부분에서개선할수있는여지가있습니다.비율과해부학적정확성을조금더현실감있게조절하고,동작과자세를더욱역동적으로표현하며,디테일과정확성을더높이고,조명과그림자의사용을더욱정교하게하며,색채와채색의깊이를더욱다채롭게만들면그림의완성도가한층더높아질것입니다.\"}},{\"feedback_type\":\"인체이해도\",\"feedback_content\":{\"grade\":5,\"content\":[{\"title\":\"머리와몸통비율\",\"detail\":\"애니메이션스타일로그려진캐릭터라비율이다소과장되어있습니다.현실적인인체비율을중시한다면머리가너무커보일수있습니다.\"},{\"title\":\"팔과다리비율\",\"detail\":\"다리길이는적절하지만,팔이조금짧아보일수있습니다.팔의길이가조금더길면더욱자연스러울수있습니다.\"},{\"title\":\"해부학적디테일\",\"detail\":\"어깨와팔의근육표현이다소부드럽게표현되어현실적인근육의윤곽이덜드러날수있습니다.해부학적정확성을높이기위해근육의윤곽을더분명하게표현하는것이좋습니다.\"},{\"title\":\"손과발의크기와형태\",\"detail\":\"손가락의크기와위치는잘표현되었으나,손의전체적인크기가조금작게보일수있습니다.손의크기를조금더키우면비율이더자연스러울수있습니다.\"}],\"recomment_reference\":[\"https://picsum.photos/id/88/180/160\",\"https://picsum.photos/id/50/260/240\",\"https://picsum.photos/id/55/260/280\",\"https://picsum.photos/id/99/100/100\",\"https://picsum.photos/id/69/160/160\"]}},{\"feedback_type\":\"동작과자세\",\"feedback_content\":{\"grade\":5,\"content\":[{\"title\":\"포즈의자연스러움\",\"detail\":\"포즈는동적이지만,다소과장된느낌이있습니다.현실적인치어리더의동작을참고하면더욱자연스러운포즈가될수있습니다.\"},{\"title\":\"몸의균형\",\"detail\":\"몸의중심이잘잡혀있지만,다리의위치가조금더다양하게표현되면동작의역동성이더해질수있습니다.\"},{\"title\":\"팔과다리의움직임\",\"detail\":\"팔과다리의움직임이자연스럽지만,팔의각도가조금더역동적이면좋겠습니다.팔을조금더높이들어올리거나,손의위치를조정하면더욱생동감이느껴질수있습니다.\"},{\"title\":\"전체적인자세\",\"detail\":\"전체적인자세가자연스럽지만,약간의비틀림이나자세의변화를주어더욱흥미로운포즈를만들수있습니다.\"}],\"recomment_reference\":[\"https://picsum.photos/id/88/180/160\",\"https://picsum.photos/id/50/260/240\",\"https://picsum.photos/id/55/260/280\",\"https://picsum.photos/id/99/100/100\",\"https://picsum.photos/id/69/160/160\"]}}]";
        return CompletableFuture.completedFuture(dummyFeedback);

    }
}
