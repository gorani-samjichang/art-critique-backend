package com.gorani_samjichang.art_critique.common;

import com.gorani_samjichang.art_critique.common.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleNoUser(final UserNotFoundException e) {
        return e.getMessage();
    }

    @ExceptionHandler(UserNotValidException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleInvalidUser(final UserNotValidException e) {
        return e.getMessage();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleNullEssentialParameter(final MissingServletRequestParameterException e) {
        return e.getMessage();
    }

    @ExceptionHandler(ServiceNotAvailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleServiceNotWorking(final ServiceNotAvailableException e) {
        return e.getMessage();
    }

    @ExceptionHandler(CannotFindBySerialNumberException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleCannotFindBySerialNumber(final CannotFindBySerialNumberException e) {
        return e.getMessage();
    }

    @ExceptionHandler(NoPermissionException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleNoPermission(final NoPermissionException e) {
        return e.getMessage();
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String BadRequestException(final BadRequestException e) {
        return e.getMessage();
    }
    // Todo: XUserNotFoundExceiptionHandler
    // Todo: BadFeeedbackRequest
    // Todo: UnsupportedEncodingException
}
