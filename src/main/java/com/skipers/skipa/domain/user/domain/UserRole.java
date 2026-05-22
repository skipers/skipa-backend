package com.skipers.skipa.domain.user.domain;

import java.util.Arrays;

public enum UserRole {
    ADMIN,
    LEGAL,
    BUSINESS;

    public String getAuthority() {
        return "ROLE_" + name();
    }

    public static UserRole from(String value) {
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid user role: " + value));
    }
}
