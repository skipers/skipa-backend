package com.skipers.skipa.domain.department.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class DepartmentException extends BusinessException {

    public DepartmentException(ErrorCode errorCode) {
        super(errorCode);
    }
}
