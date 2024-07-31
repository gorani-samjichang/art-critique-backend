package com.gorani_samjichang.art_critique.common.exceptions;

// 허가되지 않은 접근.
public class NoPermissionException extends RuntimeException {
    public NoPermissionException(String message) {
        super(message);
    }
}
