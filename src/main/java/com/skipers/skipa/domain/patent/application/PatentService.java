package com.skipers.skipa.domain.patent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.opinion.dao.OpinionSubmissionRepository;
import com.skipers.skipa.domain.patent.dao.PatentDepartmentRepository;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.dao.AnnuityHistoryRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.request.PatentCreateRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentUpdateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.PatentListResponse;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatentService {

    private final PatentRepository patentRepository;
    private final PatentDepartmentRepository patentDepartmentRepository;
    private final PatentLegalStatusRepository patentLegalStatusRepository;
    private final AnnuityHistoryRepository annuityHistoryRepository;
    private final OpinionSubmissionRepository opinionSubmissionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PatentDetailResponse create(PatentCreateRequest request) {
        String applicationNumber = request.applicationNumber();

        if (patentRepository.existsByApplicationNumber(applicationNumber)) {
            throw new PatentException(ErrorCode.DUPLICATE_APPLICATION_NUMBER);
        }

        Patent patent = patentRepository.save(Patent.builder()
                .title(request.title())
                .applicationNumber(applicationNumber)
                .registrationNumber(request.registrationNumber())
                .publicationNumber(request.publicationNumber())
                .announcementNumber(request.announcementNumber())
                .applicationDate(request.applicationDate())
                .registrationDate(request.registrationDate())
                .publicationDate(request.publicationDate())
                .announcementDate(request.announcementDate())
                .ipcCode(request.ipcCode())
                .cpcCode(request.cpcCode())
                .applicant(request.applicant())
                .inventor(request.inventor())
                .expiryDate(request.expiryDate())
                .citationCount(request.citationCount())
                .originalPdfKey(request.originalPdfKey())
                .managementNumber(request.managementNumber())
                .businessField(request.businessField())
                .techField(request.techField())
                .relatedProducts(toJsonOrNull(request.relatedProducts()))
                .filingCountry(request.filingCountry())
                .isJointApplication(request.isJointApplication())
                .jointApplicant(request.jointApplicant())
                .initialDepartment(request.initialDepartment())
                .keywords(toJsonOrNull(request.keywords()))
                .overview(request.overview())
                .coreContent(request.coreContent())
                .build());

        return toDetailResponse(patent);
    }

    public PatentDetailResponse get(Long patentId) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        return toDetailResponse(patent);
    }

    public Page<PatentListResponse> getAll(String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<PatentListResponse> result = normalizedKeyword == null
                ? patentRepository.findAll(sortedPageable).map(PatentListResponse::from)
                : patentRepository.findByTitleContainingIgnoreCase(normalizedKeyword, sortedPageable).map(PatentListResponse::from);

        return result;
    }

    @Transactional
    public PatentDetailResponse update(Long patentId, PatentUpdateRequest request) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));
        String applicationNumber = request.applicationNumber();

        if (!patent.getApplicationNumber().equals(applicationNumber)
                && patentRepository.existsByApplicationNumber(applicationNumber)) {
            throw new PatentException(ErrorCode.DUPLICATE_APPLICATION_NUMBER);
        }

        patent.update(
                request.title(),
                applicationNumber,
                request.registrationNumber(),
                request.publicationNumber(),
                request.announcementNumber(),
                request.applicationDate(),
                request.registrationDate(),
                request.publicationDate(),
                request.announcementDate(),
                request.ipcCode(),
                request.cpcCode(),
                request.applicant(),
                request.inventor(),
                request.expiryDate(),
                request.citationCount(),
                request.originalPdfKey(),
                request.managementNumber(),
                request.businessField(),
                request.techField(),
                toJsonOrNull(request.relatedProducts()),
                request.filingCountry(),
                request.isJointApplication(),
                request.jointApplicant(),
                request.initialDepartment(),
                toJsonOrNull(request.keywords()),
                request.overview(),
                request.coreContent()
        );

        return toDetailResponse(patent);
    }

    @Transactional
    public void delete(Long patentId) {
        if (!patentRepository.existsById(patentId)) {
            throw new PatentException(ErrorCode.PATENT_NOT_FOUND);
        }

        patentDepartmentRepository.deleteAllByPatentId(patentId);
        patentLegalStatusRepository.deleteAllByPatentId(patentId); // 권리 상태 이력
        annuityHistoryRepository.deleteAllByPatentId(patentId); // 연차료 납부 이력
        opinionSubmissionRepository.deleteAllByPatentId(patentId); // 사업부 의견 제출
        patentRepository.deleteById(patentId);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String toJsonOrNull(List<String> value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private List<String> fromJsonOrNull(String value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.readerForListOf(String.class).readValue(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private PatentDetailResponse toDetailResponse(Patent patent) {
        return PatentDetailResponse.from(
                patent,
                fromJsonOrNull(patent.getRelatedProducts()),
                fromJsonOrNull(patent.getKeywords())
        );
    }
}
