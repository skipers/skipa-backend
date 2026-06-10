package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.ExpiringPatentService;
import com.skipers.skipa.domain.patent.dto.response.ExpiringPatentCalendarResponse;
import com.skipers.skipa.domain.patent.dto.response.ExpiringPatentItemResponse;
import com.skipers.skipa.domain.patent.dto.response.ExpiringPatentSummaryResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import com.skipers.skipa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patents/expiring")
public class ExpiringPatentController {

    private final ExpiringPatentService expiringPatentService;

    /**
     * 소멸 예정 특허 요약을 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @return 소멸 예정 특허 요약
     */
    @Operation(
            summary = "[Common] 소멸 예정 특허 요약 조회",
            description = "3개월, 6개월, 1년, 3년, 5년 기준 소멸 예정 특허의 기술 분야별 개수를 조회합니다. "
                    + "BUSINESS 사용자는 본인 소속 부서 특허만 집계합니다."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping("/summary")
    public ApiResponse<ExpiringPatentSummaryResponse> getSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ok(expiringPatentService.getSummary(userDetails.getUser()));
    }

    /**
     * 소멸 예정 특허 목록을 조회한다(page/size 기반).
     *
     * @param userDetails 인증 사용자 정보
     * @param months 조회 기간(개월, 선택)
     * @param pageable page/size 정보
     * @return 소멸 예정 특허 목록 페이지
     */
    @Operation(
            summary = "[Common] 소멸 예정 특허 목록 조회",
            description = "선택 기간 내 소멸 예정인 특허 목록을 Patent.expiryDate 기준으로 페이지 단위 조회합니다. "
                    + "필터: months(개월 수, 미지정 시 60개월 기준). "
                    + "정렬: 소멸일 오름차순 고정입니다. "
                    + "BUSINESS 사용자는 본인 소속 부서 특허만 조회합니다."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<ExpiringPatentItemResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer months,
            @PageableDefault(page = 0, size = 50) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(expiringPatentService.getAll(
                userDetails.getUser(),
                months,
                pageable
        )));
    }

    /**
     * 소멸 예정 특허 캘린더를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param year 조회 연도(선택, 미지정 시 현재 연도)
     * @return 소멸 예정 특허 캘린더
     */
    @Operation(
            summary = "[Common] 소멸 예정 특허 캘린더 조회",
            description = "선택 연도의 월별 소멸 예정 특허 개수와 특허 목록을 Patent.expiryDate 기준으로 조회합니다. "
                    + "특허 목록에는 특허 ID, 특허명, 출원번호, 소멸 예정일, 관련 기술 분야를 포함합니다. "
                    + "필터: year(연도, 미지정 시 현재 연도). "
                    + "BUSINESS 사용자는 본인 소속 부서 특허만 조회합니다."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping("/calendar")
    public ApiResponse<ExpiringPatentCalendarResponse> getCalendar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year
    ) {
        return ApiResponse.ok(expiringPatentService.getCalendar(userDetails.getUser(), year));
    }
}
