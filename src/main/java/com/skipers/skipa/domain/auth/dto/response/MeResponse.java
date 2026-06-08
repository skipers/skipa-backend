package com.skipers.skipa.domain.auth.dto.response;

import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.dto.response.UserResponse;

public record MeResponse(
        UserResponse user
) {

    public static MeResponse from(User user) {
        return new MeResponse(UserResponse.from(user));
    }
}
