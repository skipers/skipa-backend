package com.skipers.skipa.domain.patent_legal_status.application;

import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.patent_legal_status.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent_legal_status.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent_legal_status.dto.request.PatentLegalStatusCreateRequest;
import com.skipers.skipa.domain.patent_legal_status.dto.response.PatentLegalStatusResponse;
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
    public PatentLegalStatusResponse create(PatentLegalStatusCreateRequest request) {
        Patent patent = patentRepository.findById(request.patentId())
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        PatentLegalStatus patentLegalStatus = patentLegalStatusRepository.save(PatentLegalStatus.builder()
                .patent(patent)
                .status(request.status())
                .changedAt(request.changedAt())
                .build());

        return PatentLegalStatusResponse.from(patentLegalStatus);
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

