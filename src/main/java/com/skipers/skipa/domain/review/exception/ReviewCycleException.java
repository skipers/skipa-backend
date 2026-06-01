package com.skipers.skipa.domain.review.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class ReviewCycleException extends BusinessException {

    public ReviewCycleException(ErrorCode errorCode) {
        super(errorCode);
    }
}
