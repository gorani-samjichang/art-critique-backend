package com.gorani_samjichang.art_critique.common.exceptions;

// 피드백 서비스 등이 모종의 이유로 이용 불가능.
public class ServiceNotAvailableException extends RuntimeException {
    public ServiceNotAvailableException(String message) {
        super(message);
    }
}
