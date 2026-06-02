package com.skipers.skipa.domain.review.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class ReviewException extends BusinessException {

    public ReviewException(ErrorCode errorCode) {
        super(errorCode);
    }
}
