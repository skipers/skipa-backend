package com.skipers.skipa.domain.opinion.application;

import com.skipers.skipa.domain.opinion.dao.OpinionSubmissionRepository;
import com.skipers.skipa.domain.opinion.domain.BusinessOpinion;
import com.skipers.skipa.domain.opinion.domain.OpinionSubmission;
import com.skipers.skipa.domain.opinion.domain.OpinionSubmissionStatus;
import com.skipers.skipa.domain.opinion.dto.request.OpinionSubmissionSubmitRequest;
import com.skipers.skipa.domain.opinion.dto.response.OpinionSubmissionDetailResponse;
import com.skipers.skipa.domain.opinion.dto.response.OpinionSubmissionResponse;
import com.skipers.skipa.domain.opinion.exception.OpinionSubmissionException;
import com.skipers.skipa.domain.patent.application.PatentService;
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
public class OpinionSubmissionService {

    private final OpinionSubmissionRepository opinionSubmissionRepository;
    private final PatentService patentService;

    public Page<OpinionSubmissionResponse> getAll(User user, Pageable pageable) {
        Long departmentId = getDepartmentId(user);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return opinionSubmissionRepository.findByDepartmentId(departmentId, sortedPageable)
                .map(OpinionSubmissionResponse::from);
    }

    public OpinionSubmissionDetailResponse get(User user, Long opinionSubmissionId) {
        OpinionSubmission opinionSubmission = getOwnedSubmission(user, opinionSubmissionId);

        return OpinionSubmissionDetailResponse.from(
                opinionSubmission,
                patentService.get(opinionSubmission.getPatent().getId())
        );
    }

    @Transactional
    public OpinionSubmissionResponse submit(
            User user,
            Long opinionSubmissionId,
            OpinionSubmissionSubmitRequest request
    ) {
        OpinionSubmission opinionSubmission = getOwnedSubmission(user, opinionSubmissionId);

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

        return OpinionSubmissionResponse.from(opinionSubmission);
    }

    private OpinionSubmission getOwnedSubmission(User user, Long opinionSubmissionId) {
        Long departmentId = getDepartmentId(user);

        return opinionSubmissionRepository.findByIdAndDepartmentId(opinionSubmissionId, departmentId)
                .orElseThrow(() -> opinionSubmissionRepository.existsById(opinionSubmissionId)
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
