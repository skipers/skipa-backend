package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
import com.skipers.skipa.domain.patent.dto.request.PatentLegalStatusCreateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentLegalStatusResponse;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatentLegalStatusService {

    private final PatentLegalStatusRepository patentLegalStatusRepository;
    private final PatentRepository patentRepository;

    @Transactional
    public PatentLegalStatusResponse create(Long patentId, PatentLegalStatusCreateRequest request) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        PatentLegalStatusType status;
        try {
            status = PatentLegalStatusType.valueOf(request.status());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        PatentLegalStatus legalStatus = patentLegalStatusRepository.save(PatentLegalStatus.builder()
                .patent(patent)
                .status(status)
                .changedAt(request.changedAt())
                .build());

        return PatentLegalStatusResponse.from(legalStatus);
    }

    public Page<PatentLegalStatusResponse> getAll(Long patentId, Pageable pageable) {
        if (!patentRepository.existsById(patentId)) {
            throw new PatentException(ErrorCode.PATENT_NOT_FOUND);
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return patentLegalStatusRepository.findByPatentId(patentId, sortedPageable).map(PatentLegalStatusResponse::from);
    }
}
