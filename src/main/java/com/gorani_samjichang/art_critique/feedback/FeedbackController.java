package com.gorani_samjichang.art_critique.feedback;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {
    @GetMapping("/goodImage")
    ArrayList<String> getGoodImage() {
        ArrayList<String> data = new ArrayList<>();
        for (int i = 0; i < 15; i++)
            data.add(String.format("https://picsum.photos/id/" + (int)(Math.floor(Math.random() * (100 + 1))) + "/" + (int)((Math.floor(Math.random() * (10 + 1)) + 5)*20) + "/" + (int)((Math.floor(Math.random() * (10 + 1)) + 5)*20)));
        return data;
    }

    @PostMapping("/requestFeedback")
    String requestFeedback(@RequestParam("image") MultipartFile imageFile) throws IOException {
        System.out.println(imageFile);
        return "왔음!";
    }
}
