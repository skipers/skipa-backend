package com.skipers.skipa.domain.opinion.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class OpinionSubmissionException extends BusinessException {

    public OpinionSubmissionException(ErrorCode errorCode) {
        super(errorCode);
    }
}
