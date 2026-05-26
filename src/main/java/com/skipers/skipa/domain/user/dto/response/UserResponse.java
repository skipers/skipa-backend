package com.skipers.skipa.domain.user.dto.response;

import com.skipers.skipa.domain.user.domain.User;

public record UserResponse(
        Long id,
        String loginId,
        String name,
        String email,
        String role,
        String status,
        Long departmentId,
        String departmentName
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getDepartment() != null ? user.getDepartment().getName() : null
        );
    }
}
