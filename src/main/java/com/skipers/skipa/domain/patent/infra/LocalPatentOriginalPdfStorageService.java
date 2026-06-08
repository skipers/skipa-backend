package com.skipers.skipa.domain.patent.infra;

import com.skipers.skipa.domain.patent.application.PatentOriginalPdfStorageService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalPatentOriginalPdfStorageService implements PatentOriginalPdfStorageService {

    @Override
    public void copy(String sourceObjectKey, String targetObjectKey) {
        // Local profile runs without MinIO.
    }
}
