package com.skipers.skipa.domain.patentextract.infra;

import com.skipers.skipa.domain.patentextract.application.PatentExtractStorageService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalPatentExtractStorageService implements PatentExtractStorageService {

    @Override
    public String generateUploadPresignedUrl(String objectKey) {
        return "http://localhost:9000/skipa/%s?local=true".formatted(objectKey);
    }

    @Override
    public boolean exists(String objectKey) {
        return true;
    }
}
