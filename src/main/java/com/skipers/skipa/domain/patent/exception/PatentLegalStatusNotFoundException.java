package com.skipers.skipa.domain.patent.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class PatentLegalStatusNotFoundException extends BusinessException { // 특허 권리상태 이력 미존재 예외

    public PatentLegalStatusNotFoundException() {
        super(ErrorCode.PATENT_LEGAL_STATUS_NOT_FOUND); // 표준 에러 코드 지정
    }
}

