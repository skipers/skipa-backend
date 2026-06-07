package com.skipers.skipa.domain.report.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class ReportException extends BusinessException {

    public ReportException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ReportException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
