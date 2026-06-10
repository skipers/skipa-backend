package com.skipers.skipa.domain.preevaluation.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class PreEvaluationException extends BusinessException {

    public PreEvaluationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PreEvaluationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
