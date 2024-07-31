package com.gorani_samjichang.art_critique.common.exceptions;

// 사용자가 유효하지 않음.
public class UserNotValidException extends RuntimeException {
    public UserNotValidException(String message) {
        super(message);
    }
}
