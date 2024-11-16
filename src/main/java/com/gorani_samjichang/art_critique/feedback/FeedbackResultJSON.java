package com.gorani_samjichang.art_critique.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gorani_samjichang.art_critique.study.StudyInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResultJSON{
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class Evaluation{
        @JsonProperty("평가명")
        private String name;
        @JsonProperty("점수")
        private int score;
        @JsonProperty("설명")
        private String explain;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class ReferenceImagesInfo{
        private String title;
        private String url;
        @JsonProperty("image_url")
        private String imageUrl;
    }
    @JsonProperty("분류")
    private String category;
    @JsonProperty("평가들")
    private List<Evaluation> evaluations;
    @JsonProperty("score")
    private int score;
    @JsonProperty("study")
    private List<StudyInfoDTO> studies;
    @JsonProperty("reference_image")
    private List<ReferenceImagesInfo> images;
}