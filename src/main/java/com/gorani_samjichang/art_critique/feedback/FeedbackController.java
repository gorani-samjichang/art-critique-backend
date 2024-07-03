package com.gorani_samjichang.art_critique.feedback;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
