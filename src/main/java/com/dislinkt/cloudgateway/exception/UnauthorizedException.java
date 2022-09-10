package com.dislinkt.cloudgateway.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends Exception {

    public UnauthorizedException(String errorMessage) {
        super(errorMessage);
    }
}