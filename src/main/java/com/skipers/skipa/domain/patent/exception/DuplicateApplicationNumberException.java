package com.skipers.skipa.domain.patent.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class DuplicateApplicationNumberException extends BusinessException { // 출원번호 중복 예외

    public DuplicateApplicationNumberException() {
        super(ErrorCode.DUPLICATE_APPLICATION_NUMBER); // 표준 에러 코드 지정
    }
}

