package com.skipers.skipa.domain.report.application;

import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.application.BusinessPatentAccessValidator;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import com.skipers.skipa.domain.report.dto.response.ReportCreateResponse;
import com.skipers.skipa.domain.report.dto.response.ReportResponse;
import com.skipers.skipa.domain.report.dto.response.ReportStatusResponse;
import com.skipers.skipa.domain.report.exception.ReportException;
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
public class ReportService {

    private final ReportRepository reportRepository;
    private final PatentRepository patentRepository;
    private final BusinessPatentAccessValidator businessPatentAccessValidator;

    @Transactional
    public ReportCreateResponse create(Long patentId) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        Report report = reportRepository.save(Report.builder()
                .patent(patent)
                .status(ReportStatus.생성중)
                .build());

        return ReportCreateResponse.from(report);
    }

    public Page<ReportResponse> getAll(User user, Long patentId, Pageable pageable) {
        businessPatentAccessValidator.validate(user, patentId);

        if (!patentRepository.existsById(patentId)) {
            throw new PatentException(ErrorCode.PATENT_NOT_FOUND);
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return reportRepository.findByPatentId(patentId, sortedPageable).map(ReportResponse::from);
    }

    public ReportResponse get(User user, Long patentId, Long reportId) {
        businessPatentAccessValidator.validate(user, patentId);

        Report report = reportRepository.findByIdAndPatentId(reportId, patentId)
                .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        return ReportResponse.from(report);
    }

    public ReportStatusResponse getStatus(User user, Long patentId, Long reportId) {
        businessPatentAccessValidator.validate(user, patentId);

        Report report = reportRepository.findByIdAndPatentId(reportId, patentId)
                .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        return ReportStatusResponse.from(report);
    }
}
