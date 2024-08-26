package com.gorani_samjichang.art_critique.common.imageservice;

public enum S3Paths {
    PROFILE("profile/"),
    FEEDBACK_IMAGE("img/");


    private final String path;

    S3Paths(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
