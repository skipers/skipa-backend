package com.skipers.skipa.domain.patentextract.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class PatentExtractException extends BusinessException {

    public PatentExtractException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PatentExtractException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
