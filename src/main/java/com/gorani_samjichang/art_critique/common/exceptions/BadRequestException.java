package com.gorani_samjichang.art_critique.common.exceptions;

public class BadRequestException  extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}