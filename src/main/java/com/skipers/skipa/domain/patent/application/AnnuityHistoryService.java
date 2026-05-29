package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.AnnuityHistoryRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.AnnuityHistory;
import com.skipers.skipa.domain.patent.domain.AnnuityStatus;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.request.AnnuityCreateRequest;
import com.skipers.skipa.domain.patent.dto.response.AnnuityResponse;
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
public class AnnuityHistoryService {

    private final AnnuityHistoryRepository annuityHistoryRepository;
    private final PatentRepository patentRepository;

    @Transactional
    public AnnuityResponse create(Long patentId, AnnuityCreateRequest request) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        AnnuityStatus status;
        try {
            status = AnnuityStatus.valueOf(request.status());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        AnnuityHistory annuityHistory = annuityHistoryRepository.save(AnnuityHistory.builder()
                .patent(patent)
                .annuityYear(request.annuityYear())
                .dueDate(request.dueDate())
                .paidDate(request.paidDate())
                .status(status)
                .amount(request.amount())
                .build());

        return AnnuityResponse.from(annuityHistory);
    }

    public Page<AnnuityResponse> getAll(Long patentId, Pageable pageable) {
        if (!patentRepository.existsById(patentId)) {
            throw new PatentException(ErrorCode.PATENT_NOT_FOUND);
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return annuityHistoryRepository.findByPatentId(patentId, sortedPageable).map(AnnuityResponse::from);
    }
}
