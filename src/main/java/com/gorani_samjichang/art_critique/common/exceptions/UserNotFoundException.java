package com.gorani_samjichang.art_critique.common.exceptions;

// 사용자를 찾을 수 없음.
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
