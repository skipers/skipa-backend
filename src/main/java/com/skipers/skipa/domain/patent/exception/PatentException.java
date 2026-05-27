package com.skipers.skipa.domain.patent.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class PatentException extends BusinessException {

    public PatentException(ErrorCode errorCode) {
        super(errorCode);
    }
}
