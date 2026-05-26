package com.skipers.skipa.domain.patent.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class PatentDepartmentNotFoundException extends BusinessException { // 특허 담당부서 정보 미존재 예외

    public PatentDepartmentNotFoundException() {
        super(ErrorCode.PATENT_DEPARTMENT_NOT_FOUND); // 표준 에러 코드 지정
    }
}

