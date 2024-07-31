package com.gorani_samjichang.art_critique.common.exceptions;

// 메세지를 보낼 수 없음.
public class MessagingException  extends RuntimeException {
    public MessagingException(String message) {
        super(message);
    }
}