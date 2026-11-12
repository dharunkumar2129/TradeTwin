package com.tradetwin.common.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessRuleException(String message, List<String> details) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, details);
    }
}
