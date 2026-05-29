package com.skipers.skipa.domain.patent_legal_status.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class PatentLegalStatusException extends BusinessException {

    public PatentLegalStatusException(ErrorCode errorCode) {
        super(errorCode);
    }
}

