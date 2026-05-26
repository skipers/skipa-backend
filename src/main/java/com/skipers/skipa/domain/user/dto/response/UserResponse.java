package com.skipers.skipa.domain.user.dto.response;

public record UserResponse(
        String loginId,
        String name,
        String status
) {

    public static UserResponse from(com.skipers.skipa.domain.user.domain.User user) {
        return new UserResponse(
                user.getLoginId(),
                user.getName(),
                user.getStatus().name()
        );
    }
}
