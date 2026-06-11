package com.skipers.skipa.domain.preevaluation.application;

import com.skipers.skipa.domain.chat.dao.ChatMessageRepository;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
import com.skipers.skipa.domain.preevaluation.dao.PreEvaluationRepository;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationCreateRequest;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationCreateResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationDetailResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationStatusResponse;
import com.skipers.skipa.domain.preevaluation.exception.PreEvaluationException;
import com.skipers.skipa.domain.report.application.ReportStorageService;
import com.skipers.skipa.domain.user.domain.User;
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
public class PreEvaluationService {

    private final PreEvaluationRepository preEvaluationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PreEvaluationGenerationPublisher generationPublisher;
    private final ReportStorageService reportStorageService;

    @Transactional
    public PreEvaluationCreateResponse create(User user, PreEvaluationCreateRequest request) {
        PreEvaluation preEvaluation = preEvaluationRepository.save(PreEvaluation.builder()
                .user(user)
                .title(request.title())
                .technicalDescription(request.technicalDescription())
                .claims(request.claims())
                .relatedBusiness(request.relatedBusiness())
                .targetCountries(request.targetCountries())
                .build());

        publishGenerationMessage(preEvaluation);

        return PreEvaluationCreateResponse.from(preEvaluation);
    }

    public Page<PreEvaluationResponse> getAll(User user, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return preEvaluationRepository.findByUserId(user.getId(), sortedPageable)
                .map(preEvaluation -> PreEvaluationResponse.of(preEvaluation, generateReportUrl(preEvaluation)));
    }

    public PreEvaluationDetailResponse get(User user, Long preEvaluationId) {
        PreEvaluation preEvaluation = getOwnedPreEvaluation(user, preEvaluationId);
        return PreEvaluationDetailResponse.of(preEvaluation, generateReportUrl(preEvaluation));
    }

    public PreEvaluationStatusResponse getStatus(User user, Long preEvaluationId) {
        return PreEvaluationStatusResponse.from(getOwnedPreEvaluation(user, preEvaluationId));
    }

    @Transactional
    public void delete(User user, Long preEvaluationId) {
        PreEvaluation preEvaluation = getOwnedPreEvaluation(user, preEvaluationId);
        chatMessageRepository.deleteAllByTargetTypeAndTargetId(ChatTargetType.PRE_EVALUATION, preEvaluation.getId());
        preEvaluationRepository.delete(preEvaluation);
    }

    @Transactional
    public PreEvaluationStatusResponse complete(Long preEvaluationId, String reportKey) {
        PreEvaluation preEvaluation = getPreEvaluation(preEvaluationId);
        preEvaluation.complete(reportKey, null);

        return PreEvaluationStatusResponse.from(preEvaluation);
    }

    @Transactional
    public PreEvaluationStatusResponse fail(Long preEvaluationId) {
        PreEvaluation preEvaluation = getPreEvaluation(preEvaluationId);
        preEvaluation.fail();

        return PreEvaluationStatusResponse.from(preEvaluation);
    }

    private PreEvaluation getOwnedPreEvaluation(User user, Long preEvaluationId) {
        return preEvaluationRepository.findByIdAndUserId(preEvaluationId, user.getId())
                .orElseThrow(() -> new PreEvaluationException(ErrorCode.PRE_EVALUATION_NOT_FOUND));
    }

    private PreEvaluation getPreEvaluation(Long preEvaluationId) {
        return preEvaluationRepository.findById(preEvaluationId)
                .orElseThrow(() -> new PreEvaluationException(ErrorCode.PRE_EVALUATION_NOT_FOUND));
    }

    private void publishGenerationMessage(PreEvaluation preEvaluation) {
        try {
            generationPublisher.publish(preEvaluation);
        } catch (RuntimeException e) {
            throw new PreEvaluationException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }

    private String generateReportUrl(PreEvaluation preEvaluation) {
        if (!preEvaluation.isCompleted()) {
            return null;
        }

        return reportStorageService.generatePresignedUrl(preEvaluation.getReportKey());
    }
}
