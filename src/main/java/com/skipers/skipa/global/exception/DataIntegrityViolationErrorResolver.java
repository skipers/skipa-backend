package com.skipers.skipa.global.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataIntegrityViolationErrorResolver {

    private static final Map<String, ErrorCode> ERROR_CODE_BY_CONSTRAINT = Map.of(
            "uk_patents_application_number", ErrorCode.DUPLICATE_APPLICATION_NUMBER,
            "uk_reviews_patent_department", ErrorCode.DUPLICATE_REVIEW_REQUEST
    );

    public ErrorCode resolve(DataIntegrityViolationException exception) {
        ConstraintViolationException constraintViolation = findConstraintViolation(exception);
        if (constraintViolation == null) {
            return ErrorCode.CONFLICT;
        }

        return ERROR_CODE_BY_CONSTRAINT.getOrDefault(
                constraintViolation.getConstraintName(),
                ErrorCode.CONFLICT
        );
    }

    private ConstraintViolationException findConstraintViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException) {
                return constraintViolationException;
            }
            current = current.getCause();
        }
        return null;
    }
}
