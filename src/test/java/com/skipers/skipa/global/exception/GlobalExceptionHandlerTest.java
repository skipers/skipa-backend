package com.skipers.skipa.global.exception;

import com.skipers.skipa.global.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            new DataIntegrityViolationErrorResolver()
    );

    @Test
    void businessExceptionPreservesErrorCode() {
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.DUPLICATE_EMAIL)
        );

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getCode()).isEqualTo("DUPLICATE_EMAIL");
    }

    @Test
    void validationExceptionUsesFirstFieldErrorMessage() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(new FieldError("request", "email", "email is invalid"))
        );

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("email is invalid");
    }

    @Test
    void validationExceptionUsesDefaultMessageWhenNoFieldErrorExists() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getMessage()).isEqualTo(ErrorCode.INVALID_REQUEST.getMessage());
    }

    @Test
    void unreadableRequestReturnsInvalidRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(
                mock(HttpMessageNotReadableException.class)
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("요청 본문(JSON) 형식이 올바르지 않습니다.");
    }

    @Test
    void typeMismatchReturnsInvalidRequest() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("patentId");
        when(exception.getValue()).thenReturn("not-a-number");

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatchException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("요청 파라미터 타입이 올바르지 않습니다.");
    }

    @Test
    void dataIntegrityViolationReturnsResolvedErrorCode() {
        DataIntegrityViolationErrorResolver resolver = mock(DataIntegrityViolationErrorResolver.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(resolver);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate application number");
        when(resolver.resolve(exception)).thenReturn(ErrorCode.DUPLICATE_APPLICATION_NUMBER);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getError().getCode()).isEqualTo("DUPLICATE_APPLICATION_NUMBER");
    }

    @Test
    void unknownExceptionReturnsInternalError() {
        ResponseEntity<ErrorResponse> response = handler.handleException(new RuntimeException("unknown"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void accessDeniedExceptionIsDelegatedToSecurityHandling() {
        AccessDeniedException exception = new AccessDeniedException("denied");

        assertThatThrownBy(() -> handler.handleException(exception))
                .isSameAs(exception);
    }
}
