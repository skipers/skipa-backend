package com.skipers.skipa.domain.user.api;

import com.skipers.skipa.domain.user.application.AdminUserService;
import com.skipers.skipa.domain.user.dto.request.UserApproveRequest;
import com.skipers.skipa.domain.user.dto.response.UserResponse;
import com.skipers.skipa.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 가입 승인 대기 중인 사용자를 승인하고 부서를 지정한다.
     *
     * @param userId 승인할 사용자 ID
     * @param request 부서 ID
     * @return 승인된 사용자 정보
     */
    @Operation(summary = "[Admin] 사용자 가입 승인", description = "승인 대기 중인 사용자를 승인하고 소속 부서를 지정합니다.")
    @PatchMapping("/{userId}/approve")
    public ApiResponse<UserResponse> approve(
            @PathVariable Long userId,
            @Valid @RequestBody UserApproveRequest request
    ) {
        return ApiResponse.ok(adminUserService.approve(userId, request));
    }
}
