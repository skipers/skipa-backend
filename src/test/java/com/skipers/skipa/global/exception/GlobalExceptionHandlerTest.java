package com.skipers.skipa.global.exception;

import com.skipers.skipa.global.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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
        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(
                mock(HttpMessageNotReadableException.class)
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void authorizationDeniedReturnsForbidden() {
        ResponseEntity<ErrorResponse> response = handler.handleAuthorizationDeniedException(
                new AuthorizationDeniedException("denied")
        );

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getError().getCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void unknownExceptionReturnsInternalError() {
        ResponseEntity<ErrorResponse> response = handler.handleException(new RuntimeException("unknown"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
    }
}
