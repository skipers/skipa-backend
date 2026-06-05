package com.skipers.skipa.global.response;

import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseTest {

    @Test
    void apiResponseWrapsSuccessDataAndVoidSuccess() {
        ApiResponse<String> withData = ApiResponse.ok("data");
        ApiResponse<Void> withoutData = ApiResponse.ok();

        assertThat(withData.isSuccess()).isTrue();
        assertThat(withData.getData()).isEqualTo("data");
        assertThat(withoutData.isSuccess()).isTrue();
        assertThat(withoutData.getData()).isNull();
    }

    @Test
    void errorResponseUsesDefaultOrOverriddenMessage() {
        ErrorResponse defaultResponse = ErrorResponse.of(ErrorCode.INVALID_REQUEST);
        ErrorResponse customResponse = ErrorResponse.of(ErrorCode.INVALID_REQUEST, "custom message");

        assertThat(defaultResponse.isSuccess()).isFalse();
        assertThat(defaultResponse.getError().getCode()).isEqualTo("INVALID_REQUEST");
        assertThat(defaultResponse.getError().getMessage()).isEqualTo(ErrorCode.INVALID_REQUEST.getMessage());
        assertThat(customResponse.getError().getMessage()).isEqualTo("custom message");
    }

    @Test
    void pageResponseMapsPagingMetadata() {
        PageResponse<String> response = PageResponse.from(
                new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5)
        );

        assertThat(response.getItems()).containsExactly("a", "b");
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalItems()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.isHasPrevious()).isTrue();
    }
}
