package com.skipers.skipa.domain.report.infra;

import com.skipers.skipa.domain.report.application.ReportStorageService;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local | local-postgres")
public class LocalReportStorageService implements ReportStorageService {

    @Override
    public String generatePresignedUrl(String reportKey) {
        throw new ReportException(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }
}
