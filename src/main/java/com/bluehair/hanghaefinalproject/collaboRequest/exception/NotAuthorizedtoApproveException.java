package com.bluehair.hanghaefinalproject.collaboRequest.exception;

import com.bluehair.hanghaefinalproject.common.response.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotAuthorizedtoApproveException extends RuntimeException {
    private final ErrorCode errorCode;
}