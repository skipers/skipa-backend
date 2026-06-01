package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.opinion.dao.OpinionSubmissionRepository;
import com.skipers.skipa.domain.opinion.domain.BusinessOpinion;
import com.skipers.skipa.domain.opinion.domain.OpinionSubmission;
import com.skipers.skipa.domain.opinion.domain.OpinionSubmissionStatus;
import com.skipers.skipa.domain.opinion.dto.request.OpinionSubmissionSubmitRequest;
import com.skipers.skipa.domain.opinion.exception.OpinionSubmissionException;
import com.skipers.skipa.domain.patent.dto.response.AssignedPatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.AssignedPatentResponse;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignedPatentService {

    private final OpinionSubmissionRepository opinionSubmissionRepository;
    private final PatentService patentService;

    public Page<AssignedPatentResponse> getAll(User user, Pageable pageable) {
        Long departmentId = getDepartmentId(user);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return opinionSubmissionRepository.findByDepartmentId(departmentId, sortedPageable)
                .map(AssignedPatentResponse::from);
    }

    public AssignedPatentDetailResponse get(User user, Long patentId) {
        OpinionSubmission opinionSubmission = getOwnedSubmission(user, patentId);

        return AssignedPatentDetailResponse.of(
                patentService.get(patentId),
                opinionSubmission
        );
    }

    @Transactional
    public AssignedPatentResponse submit(
            User user,
            Long patentId,
            OpinionSubmissionSubmitRequest request
    ) {
        OpinionSubmission opinionSubmission = getOwnedSubmission(user, patentId);

        if (opinionSubmission.getStatus() == OpinionSubmissionStatus.제출완료) {
            throw new OpinionSubmissionException(ErrorCode.DECISION_ALREADY_SUBMITTED);
        }

        BusinessOpinion opinion;
        try {
            opinion = BusinessOpinion.valueOf(request.opinion());
        } catch (IllegalArgumentException e) {
            throw new OpinionSubmissionException(ErrorCode.INVALID_REQUEST);
        }

        opinionSubmission.submit(opinion, request.comment(), Instant.now());

        return AssignedPatentResponse.from(opinionSubmission);
    }

    private OpinionSubmission getOwnedSubmission(User user, Long patentId) {
        Long departmentId = getDepartmentId(user);

        return opinionSubmissionRepository.findByPatentIdAndDepartmentId(patentId, departmentId)
                .orElseThrow(() -> opinionSubmissionRepository.existsByPatentId(patentId)
                        ? new OpinionSubmissionException(ErrorCode.FORBIDDEN)
                        : new OpinionSubmissionException(ErrorCode.DECISION_NOT_FOUND));
    }

    private Long getDepartmentId(User user) {
        if (user.getDepartment() == null) {
            throw new OpinionSubmissionException(ErrorCode.FORBIDDEN);
        }

        return user.getDepartment().getId();
    }
}
