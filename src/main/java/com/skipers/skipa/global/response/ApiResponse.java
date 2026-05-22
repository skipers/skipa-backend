package com.skipers.skipa.global.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

    private boolean success;
    private T data;

    private ApiResponse(T data) {
        this.success = true;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(null);
    }
}
