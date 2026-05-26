package com.skipers.skipa.domain.patent.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class PatentNotFoundException extends BusinessException { // 특허 미존재 예외

    public PatentNotFoundException() {
        super(ErrorCode.PATENT_NOT_FOUND); // 표준 에러 코드 지정
    }
}

