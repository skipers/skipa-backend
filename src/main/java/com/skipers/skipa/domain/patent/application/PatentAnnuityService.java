package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.request.PatentAnnuityCreateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentAnnuityResponse;
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
public class PatentAnnuityService {

    private final PatentAnnuityRepository patentAnnuityRepository;
    private final PatentRepository patentRepository;

    @Transactional
    public PatentAnnuityResponse create(Long patentId, PatentAnnuityCreateRequest request) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        PatentAnnuityStatus status;
        try {
            status = PatentAnnuityStatus.valueOf(request.status());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        PatentAnnuity patentAnnuity = patentAnnuityRepository.save(PatentAnnuity.builder()
                .patent(patent)
                .annuityYear(request.annuityYear())
                .dueDate(request.dueDate())
                .paidDate(request.paidDate())
                .status(status)
                .amount(request.amount())
                .build());

        return PatentAnnuityResponse.from(patentAnnuity);
    }

    public Page<PatentAnnuityResponse> getAll(Long patentId, Pageable pageable) {
        if (!patentRepository.existsById(patentId)) {
            throw new PatentException(ErrorCode.PATENT_NOT_FOUND);
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return patentAnnuityRepository.findByPatentId(patentId, sortedPageable).map(PatentAnnuityResponse::from);
    }
}
