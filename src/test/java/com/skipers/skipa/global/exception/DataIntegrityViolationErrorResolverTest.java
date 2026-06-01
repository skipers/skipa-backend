package com.skipers.skipa.global.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataIntegrityViolationErrorResolverTest {

    private final DataIntegrityViolationErrorResolver resolver = new DataIntegrityViolationErrorResolver();

    @Test
    void applicationNumberConstraintReturnsDuplicateApplicationNumber() {
        ConstraintViolationException cause = mock(ConstraintViolationException.class);
        when(cause.getConstraintName()).thenReturn("uk_patents_application_number");

        ErrorCode result = resolver.resolve(new DataIntegrityViolationException("constraint violation", cause));

        assertThat(result).isEqualTo(ErrorCode.DUPLICATE_APPLICATION_NUMBER);
    }

    @Test
    void reviewConstraintReturnsDuplicateReviewRequest() {
        ConstraintViolationException cause = mock(ConstraintViolationException.class);
        when(cause.getConstraintName()).thenReturn("uk_reviews_patent_department");

        ErrorCode result = resolver.resolve(new DataIntegrityViolationException("constraint violation", cause));

        assertThat(result).isEqualTo(ErrorCode.DUPLICATE_REVIEW_REQUEST);
    }

    @Test
    void unknownConstraintReturnsConflict() {
        ConstraintViolationException cause = mock(ConstraintViolationException.class);
        when(cause.getConstraintName()).thenReturn("unknown_constraint");

        ErrorCode result = resolver.resolve(new DataIntegrityViolationException("constraint violation", cause));

        assertThat(result).isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void violationWithoutHibernateConstraintReturnsConflict() {
        ErrorCode result = resolver.resolve(new DataIntegrityViolationException("constraint violation"));

        assertThat(result).isEqualTo(ErrorCode.CONFLICT);
    }
}
