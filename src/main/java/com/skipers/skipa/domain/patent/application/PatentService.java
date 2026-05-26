package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.dao.PatentSpecifications;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.request.PatentCreateRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentUpdateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.PatentListResponse;
import com.skipers.skipa.domain.patent.exception.DuplicateApplicationNumberException;
import com.skipers.skipa.domain.patent.exception.PatentNotFoundException;
import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
import com.skipers.skipa.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service // 특허 유스케이스 서비스
@RequiredArgsConstructor // 생성자 주입
@Transactional(readOnly = true) // 기본은 조회 트랜잭션
public class PatentService {

    private static final int DEFAULT_PAGE_SIZE = 20; // 기본 페이지 크기
    private static final int MAX_PAGE_SIZE = 100; // 최대 페이지 크기(과도한 조회 방지)

    private final PatentRepository patentRepository; // 특허 저장소

    @Transactional // 등록은 쓰기 트랜잭션
    public PatentDetailResponse create(PatentCreateRequest request) {
        String title = normalizeRequired(request.title()); // 필수값 정규화
        String applicationNumber = normalizeRequired(request.applicationNumber()); // 필수값 정규화

        if (patentRepository.existsByApplicationNumber(applicationNumber)) { // 출원번호 중복 방지
            throw new DuplicateApplicationNumberException(); // 표준 비즈니스 예외
        }

        Patent patent = patentRepository.save(Patent.builder() // 특허 엔티티 생성/저장
                .title(title)
                .applicationNumber(applicationNumber)
                .registrationNumber(normalizeOptional(request.registrationNumber()))
                .publicationNumber(normalizeOptional(request.publicationNumber()))
                .announcementNumber(normalizeOptional(request.announcementNumber()))
                .applicationDate(request.applicationDate())
                .registrationDate(request.registrationDate())
                .publicationDate(request.publicationDate())
                .announcementDate(request.announcementDate())
                .ipcCode(normalizeOptional(request.ipcCode()))
                .cpcCode(normalizeOptional(request.cpcCode()))
                .applicant(normalizeOptional(request.applicant()))
                .inventor(normalizeOptional(request.inventor()))
                .expiryDate(request.expiryDate())
                .citationCount(request.citationCount())
                .originalPdfKey(normalizeOptional(request.originalPdfKey()))
                .managementNumber(normalizeOptional(request.managementNumber()))
                .businessField(normalizeOptional(request.businessField()))
                .techField(normalizeOptional(request.techField()))
                .relatedProducts(normalizeOptional(request.relatedProducts()))
                .filingCountry(normalizeOptional(request.filingCountry()))
                .isJointApplication(request.isJointApplication())
                .jointApplicant(normalizeOptional(request.jointApplicant()))
                .initialDepartment(null) // 최초 담당 부서는 Legal 배정 시점에만 스냅샷으로 기록(등록 단계에서는 미설정)
                .keywords(normalizeOptional(request.keywords()))
                .overview(normalizeOptional(request.overview()))
                .coreContent(normalizeOptional(request.coreContent()))
                .build());

        return PatentDetailResponse.from(patent); // 응답 DTO 변환
    }

    public PatentDetailResponse get(Long patentId) {
        Patent patent = patentRepository.findById(patentId) // ID로 단건 조회
                .orElseThrow(PatentNotFoundException::new);

        return PatentDetailResponse.from(patent); // 응답 DTO 변환
    }

    public PageResponse<PatentListResponse> search(String keyword, Integer page, Integer size) {
        int pageNumber = normalizePage(page); // page 기본값/검증
        int pageSize = normalizeSize(size); // size 기본값/검증

        Specification<Patent> specification = PatentSpecifications.titleContainsIgnoreCase(normalizeKeyword(keyword)); // v1: 제목 검색만 지원
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id")); // 기본 정렬(최신순)

        Page<PatentListResponse> result = patentRepository.findAll(specification, pageRequest) // 동적 검색 + 페이징
                .map(PatentListResponse::from);

        return PageResponse.from(result); // 공통 페이징 응답 변환
    }

    @Transactional // 수정은 쓰기 트랜잭션
    public PatentDetailResponse update(Long patentId, PatentUpdateRequest request) {
        Patent patent = patentRepository.findById(patentId) // 수정 대상 조회
                .orElseThrow(PatentNotFoundException::new);

        if (request.title() != null) { // PATCH: 전달된 필드만 반영
            String title = normalizeRequired(request.title()); // 공백/빈값 방지
            patent.updateTitle(title); // 엔티티 변경
        }

        return PatentDetailResponse.from(patent); // 변경 감지(Dirty Checking)로 반영
    }

    private int normalizePage(Integer page) { // 페이지 번호 정규화
        if (page == null) {
            return 0; // 기본 0페이지
        }
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST); // 음수 페이지 방지
        }
        return page;
    }

    private int normalizeSize(Integer size) { // 페이지 크기 정규화
        if (size == null) {
            return DEFAULT_PAGE_SIZE; // 기본 페이지 크기
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST); // 과도한 size 방지
        }
        return size;
    }

    private String normalizeRequired(String value) { // 필수 문자열 정규화(공백 제거)
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST); // 필수값 누락 방지
        }

        String normalized = value.trim(); // 양끝 공백 제거
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST); // 공백만 전달 방지
        }

        return normalized;
    }

    private String normalizeOptional(String value) { // 선택 문자열 정규화(빈문자열은 null 처리)
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeKeyword(String keyword) { // 검색어 정규화(공백만 있으면 null)
        return normalizeOptional(keyword);
    }
}
