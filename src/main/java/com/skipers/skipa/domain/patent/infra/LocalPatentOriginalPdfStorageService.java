package com.skipers.skipa.domain.patent.infra;

import com.skipers.skipa.domain.patent.application.PatentOriginalPdfStorageService;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local | local-postgres")
public class LocalPatentOriginalPdfStorageService implements PatentOriginalPdfStorageService {

    @Override
    public void copy(String sourceObjectKey, String targetObjectKey) {
        // Local profile runs without MinIO.
    }

    @Override
    public String generatePresignedUrl(String originalPdfKey) {
        throw new PatentException(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }
}
