package com.gorani_samjichang.art_critique.common.exceptions;

// 트위터에서 유저를 찾는데 실패함.
public class XUserNotFoundException extends RuntimeException{
    public XUserNotFoundException(String message) {
        super(message);
    }
}