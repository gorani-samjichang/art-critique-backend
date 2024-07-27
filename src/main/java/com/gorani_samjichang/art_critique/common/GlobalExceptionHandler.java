package com.gorani_samjichang.art_critique.common;

import com.gorani_samjichang.art_critique.common.exceptions.UserNotFoundException;
import com.gorani_samjichang.art_critique.common.exceptions.UserNotValidException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public String handleNoUser(HttpServletResponse response, final HttpClientErrorException.Forbidden e) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        return e.getMessage();
    }

    @ExceptionHandler(UserNotValidException.class)
    public String handleInvalidUser(HttpServletResponse response, final HttpClientErrorException.Unauthorized e) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return e.getMessage();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String handleNullEssentialParameter(HttpServletResponse response, final HttpClientErrorException.BadRequest e) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return e.getMessage();
    }
}
