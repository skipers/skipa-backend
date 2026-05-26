package com.skipers.skipa.domain.patent.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class AnnuityHistoryNotFoundException extends BusinessException { // 연차료 납부 이력 미존재 예외

    public AnnuityHistoryNotFoundException() {
        super(ErrorCode.ANNUITY_HISTORY_NOT_FOUND); // 표준 에러 코드 지정
    }
}

