package com.gorani_samjichang.art_critique.feedback;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FeedbackDto {
    private Total total;
    private Score score;

    @Getter
    @Setter
    public static class Total {
        private String text;
        private String level;
    }

    @Getter
    @Setter
    public static class Score {
        private List<ScoreItem> item;
    }

    @Getter
    @Setter
    public static class ScoreItem {
        private String itemName;
        private int itemScore;
    }
}
