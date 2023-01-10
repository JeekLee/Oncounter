package com.bluehair.hanghaefinalproject.common.exception;

import com.bluehair.hanghaefinalproject.common.response.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DuplicationException extends IllegalArgumentException{
    private Domain domain;
    private Layer layer;
    private ErrorCode errorCode;
}
