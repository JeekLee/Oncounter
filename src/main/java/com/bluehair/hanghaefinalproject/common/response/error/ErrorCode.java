package com.bluehair.hanghaefinalproject.common.response.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// Domain이 커지게 될 경우, 해당 객체를 Abstract로 선언 후 Domain별 extend
@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_EMAIL(HttpStatus.OK, "유효하지 않은 이메일입니다.", 4041),
    DUPLICATED_EMAIL(HttpStatus.OK, "중복된 이메일입니다.", 4042),
    MEMBER_NOT_EXIST(HttpStatus.OK, "존재하지 않는 회원입니다.", 4043);

    private final HttpStatus httpStatus;
    private final String message;
    private final Integer customHttpStatusCode;

}
