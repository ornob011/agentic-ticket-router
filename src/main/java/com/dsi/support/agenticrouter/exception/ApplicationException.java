package com.dsi.support.agenticrouter.exception;

import com.dsi.support.agenticrouter.enums.ErrorCode;
import lombok.Getter;

@Getter
public abstract class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    protected ApplicationException(
        ErrorCode errorCode,
        String detail
    ) {
        super(
            detail
        );

        this.errorCode = errorCode;
        this.detail = detail;
    }

    protected ApplicationException(
        ErrorCode errorCode,
        String detail,
        Throwable cause
    ) {
        super(
            detail,
            cause
        );

        this.errorCode = errorCode;
        this.detail = detail;
    }
}
