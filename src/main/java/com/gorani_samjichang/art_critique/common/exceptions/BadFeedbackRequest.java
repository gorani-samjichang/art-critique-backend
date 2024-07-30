package com.gorani_samjichang.art_critique.common.exceptions;

public class BadFeedbackRequest extends RuntimeException {
    public BadFeedbackRequest(String message) {
        super(message);
    }
}