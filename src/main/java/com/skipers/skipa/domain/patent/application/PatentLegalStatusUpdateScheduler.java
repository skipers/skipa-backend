package com.skipers.skipa.domain.patent.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatentLegalStatusUpdateScheduler {

    private final PatentLegalStatusUpdateService patentLegalStatusUpdateService;

    @Scheduled(cron = "0 10 8 * * *", zone = "Asia/Seoul")
    public void updateAdministrativeStatuses() {
        patentLegalStatusUpdateService.updateAdministrativeStatuses();
    }
}
