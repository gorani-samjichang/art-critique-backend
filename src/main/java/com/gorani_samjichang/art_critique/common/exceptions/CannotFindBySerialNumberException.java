package com.gorani_samjichang.art_critique.common.exceptions;

// 시리얼넘버로 찾을 수 없음. 유효하지 않은 시리얼 넘버
public class CannotFindBySerialNumberException extends RuntimeException {
    public CannotFindBySerialNumberException(String message) {
        super(message);
    }
}
