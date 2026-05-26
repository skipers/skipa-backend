package com.skipers.skipa.domain.auth.api;

import com.skipers.skipa.domain.auth.application.AuthService;
import com.skipers.skipa.domain.auth.dto.request.LoginRequest;
import com.skipers.skipa.domain.auth.dto.response.LoginResponse;
import com.skipers.skipa.domain.user.dto.request.UserCreateRequest;
import com.skipers.skipa.domain.user.dto.response.UserResponse;
import com.skipers.skipa.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입 요청을 처리한다.
     * 가입 후 계정은 PENDING 상태로 저장되며, admin 승인 후 로그인이 가능하다.
     *
     * @param request 로그인 ID, 비밀번호, 이름, 이메일, 역할, 부서 ID
     * @return 가입된 사용자의 로그인 ID, 이름, 상태
     */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    /**
     * 로그인 요청을 처리한다.
     * PENDING 상태의 계정은 로그인이 차단된다.
     *
     * @param request 로그인 ID, 비밀번호
     * @return access token, refresh token, 사용자 정보
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
