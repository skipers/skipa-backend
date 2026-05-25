package com.skipers.skipa.domain.department.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class DepartmentNotFoundException extends BusinessException {

    public DepartmentNotFoundException() {
        super(ErrorCode.DEPARTMENT_NOT_FOUND);
    }
}
