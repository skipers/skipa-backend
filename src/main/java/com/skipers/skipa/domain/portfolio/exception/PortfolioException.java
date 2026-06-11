package com.skipers.skipa.domain.portfolio.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

public class PortfolioException extends BusinessException {

    public PortfolioException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PortfolioException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
