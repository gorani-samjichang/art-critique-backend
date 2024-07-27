package com.gorani_samjichang.art_critique.common;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
    public String handleUnauthenticatedException(HttpServletResponse response, final HttpClientErrorException.Forbidden e) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return e.getMessage();
    }

    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public String handleUnauthenticatedException(HttpServletResponse response, final HttpClientErrorException.Unauthorized e) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        return e.getMessage();
    }

    @ExceptionHandler(HttpClientErrorException.BadRequest.class)
    public String handleBadRequestException(HttpServletResponse response, final HttpClientErrorException.BadRequest e) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return e.getMessage();
    }
}
