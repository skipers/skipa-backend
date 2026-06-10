package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentApprovalStatus;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovedPatentValidator {

    private final PatentRepository patentRepository;

    public Patent getApprovedPatent(Long patentId) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));
        validateApproved(patent);
        return patent;
    }

    public void validateApproved(Patent patent) {
        if (patent.getApprovalStatus() != PatentApprovalStatus.APPROVED) {
            throw new PatentException(ErrorCode.PATENT_NOT_FOUND);
        }
    }
}
