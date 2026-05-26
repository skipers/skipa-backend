package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.PatentService;
import com.skipers.skipa.domain.patent.dto.request.PatentCreateRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentUpdateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.PatentListResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController // 특허 기본 API(등록/조회/목록/수정)
@RequiredArgsConstructor // 생성자 주입
@RequestMapping("/patents") // 특허 도메인 기본 경로
public class PatentController {

    private final PatentService patentService; // 특허 유스케이스 서비스

    @PostMapping // 특허 등록
    public ResponseEntity<ApiResponse<PatentDetailResponse>> create(@Valid @RequestBody PatentCreateRequest request) { // 요청 검증(@Valid)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(patentService.create(request))); // 201 + 생성 결과 반환
    }

    @GetMapping("/{patentId}") // 특허 단건 조회
    public ApiResponse<PatentDetailResponse> get(@PathVariable Long patentId) { // path 변수로 조회 대상 지정
        return ApiResponse.ok(patentService.get(patentId)); // 단건 응답 반환
    }

    @GetMapping // 특허 목록/검색(페이징)
    public ApiResponse<PageResponse<PatentListResponse>> search(
            @RequestParam(required = false) String keyword, // v1: 제목 검색 키워드(선택)
            @RequestParam(required = false) Integer page, // 페이지 번호(0부터, 선택)
            @RequestParam(required = false) Integer size // 페이지 크기(선택)
    ) {
        return ApiResponse.ok(patentService.search(keyword, page, size)); // 페이지 응답 반환
    }

    @PatchMapping("/{patentId}") // 특허 부분 수정(PATCH)
    public ApiResponse<PatentDetailResponse> update(
            @PathVariable Long patentId, // 수정 대상 ID
            @Valid @RequestBody PatentUpdateRequest request // PATCH 요청 DTO
    ) {
        return ApiResponse.ok(patentService.update(patentId, request)); // 수정 결과 반환
    }
}
