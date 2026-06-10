package com.skipers.skipa.domain.auth.api;

import com.skipers.skipa.domain.auth.application.AuthService;
import com.skipers.skipa.domain.auth.dto.request.LoginRequest;
import com.skipers.skipa.domain.auth.dto.request.RegisterRequest;
import com.skipers.skipa.domain.auth.dto.request.TokenRefreshRequest;
import com.skipers.skipa.domain.auth.dto.response.LoginResponse;
import com.skipers.skipa.domain.auth.dto.response.MeResponse;
import com.skipers.skipa.domain.auth.dto.response.TokenRefreshResponse;
import com.skipers.skipa.domain.user.dto.response.UserResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
     * @param request 로그인 ID, 비밀번호, 이름, 이메일, 역할
     * @return 가입된 사용자 정보
     */
    @Operation(summary = "[Common] 회원가입", description = "사용자 정보를 입력받아 승인 대기 상태의 계정을 생성합니다.")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.register(request)));
    }

    /**
     * 로그인 요청을 처리한다.
     * PENDING 상태의 계정은 로그인이 차단된다.
     *
     * @param request 로그인 ID, 비밀번호
     * @return access token, refresh token, 사용자 정보
     */
    @Operation(summary = "[Common] 로그인", description = "로그인 정보를 검증하고 인증 토큰과 사용자 정보를 반환합니다.")
    @SecurityRequirements
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "[Common] 내 정보 조회", description = "현재 로그인한 사용자 정보를 반환합니다.")
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(authService.me(userDetails.getUser()));
    }

    @Operation(summary = "[Common] 액세스 토큰 재발급", description = "refresh token으로 access token을 재발급합니다.")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @Operation(summary = "[Common] 로그아웃", description = "현재 로그인한 사용자의 refresh token을 폐기합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUser());
        return ApiResponse.ok();
    }
}
