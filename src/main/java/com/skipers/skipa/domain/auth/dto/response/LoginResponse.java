package com.skipers.skipa.domain.auth.dto.response;

import com.skipers.skipa.domain.user.domain.User;

public record LoginResponse(
        String accessToken,
        UserInfo user
) {

    public static LoginResponse of(String accessToken, User user) {
        return new LoginResponse(
                accessToken,
                UserInfo.from(user)
        );
    }

    public record UserInfo(
            String loginId,
            String role,
            Long departmentId
    ) {

        public static UserInfo from(User user) {
            return new UserInfo(
                    user.getLoginId(),
                    user.getRole().name(),
                    user.getDepartment() != null ? user.getDepartment().getId() : null
            );
        }
    }
}
