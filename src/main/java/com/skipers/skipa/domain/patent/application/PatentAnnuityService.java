package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.request.PatentAnnuityCreateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentAnnuityResponse;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatentAnnuityService {

    private final PatentAnnuityRepository patentAnnuityRepository;
    private final PatentRepository patentRepository;
    private final BusinessPatentAccessValidator businessPatentAccessValidator;

    @Transactional
    public PatentAnnuityResponse create(Long patentId, PatentAnnuityCreateRequest request) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        PatentAnnuity patentAnnuity = patentAnnuityRepository
                .findFirstByPatentIdAndStatusOrderByStartYearDescIdDesc(patentId, PatentAnnuityStatus.UNPAID)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATENT_ANNUITY_NOT_FOUND));
        patentAnnuity.pay(request.paymentYears(), request.amount(), LocalDate.now());
        createNextUnpaidAnnuity(patentAnnuity, request.paymentYears());

        return PatentAnnuityResponse.from(patentAnnuity);
    }

    public Page<PatentAnnuityResponse> getAll(User user, Long patentId, Pageable pageable) {
        businessPatentAccessValidator.validate(user, patentId);

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

    private void createNextUnpaidAnnuity(PatentAnnuity paidAnnuity, int paymentYears) {
        int nextStartYear = paidAnnuity.getEndYear() + 1;
        Long patentId = paidAnnuity.getPatent().getId();
        if (patentAnnuityRepository.existsByPatentIdAndStartYear(patentId, nextStartYear)) {
            return;
        }

        patentAnnuityRepository.save(PatentAnnuity.builder()
                .patent(paidAnnuity.getPatent())
                .startYear(nextStartYear)
                .dueDate(nextDueDate(paidAnnuity, paymentYears))
                .status(PatentAnnuityStatus.UNPAID)
                .build());
    }

    private LocalDate nextDueDate(PatentAnnuity paidAnnuity, int paymentYears) {
        if (paidAnnuity.getDueDate() == null) {
            return null;
        }
        return paidAnnuity.getDueDate().plusYears(paymentYears);
    }
}
