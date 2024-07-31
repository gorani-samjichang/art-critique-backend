package com.gorani_samjichang.art_critique.common.exceptions;

// 사용자의 피드백에 대한 평가가 너무 짧거나 할 경우 발생
public class BadFeedbackRequestException extends RuntimeException {
    public BadFeedbackRequestException(String message) {
        super(message);
    }
}