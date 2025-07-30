package com.mytry.editortry.Try.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ExceptionMessage handleIllegalArgumentException(IllegalArgumentException e){

        ExceptionMessage em = new ExceptionMessage();

        em.setMessage(e.getMessage());
        return em;

    }
}
