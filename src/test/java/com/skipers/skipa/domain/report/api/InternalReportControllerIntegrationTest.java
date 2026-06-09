package com.skipers.skipa.domain.report.api;

import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class InternalReportControllerIntegrationTest {

    private static final String INTERNAL_API_KEY = "test-internal-api-key";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PatentRepository patentRepository;

    @Autowired
    private ReportRepository reportRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void completeStoresReportKeyAndMarksReportCompleted() throws Exception {
        Report report = saveGeneratingReport("APP-COMPLETE");

        mockMvc.perform(patch("/internal/reports/{reportId}/complete", report.getId())
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportKey": "reports/%d/report.html",
                                  "totalScore": 82.5
                                }
                                """.formatted(report.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(report.getId()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.totalScore").value(82.5));

        Report completedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(completedReport.getStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(completedReport.getReportKey()).isEqualTo("reports/%d/report.html".formatted(report.getId()));
        assertThat(completedReport.getTotalScore()).isEqualByComparingTo("82.50");
        assertThat(completedReport.getEvaluatedAt()).isNotNull();
    }

    @Test
    void completeRejectsBlankReportKey() throws Exception {
        Report report = saveGeneratingReport("APP-BLANK");

        mockMvc.perform(patch("/internal/reports/{reportId}/complete", report.getId())
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportKey": " ",
                                  "totalScore": 82.5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message", notNullValue()));

        Report unchangedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(unchangedReport.getStatus()).isEqualTo(ReportStatus.GENERATING);
        assertThat(unchangedReport.getReportKey()).isNull();
    }

    @Test
    void completeRejectsMissingTotalScore() throws Exception {
        Report report = saveGeneratingReport("APP-MISSING-SCORE");

        mockMvc.perform(patch("/internal/reports/{reportId}/complete", report.getId())
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportKey": "reports/%d/report.html"
                                }
                                """.formatted(report.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        Report unchangedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(unchangedReport.getStatus()).isEqualTo(ReportStatus.GENERATING);
        assertThat(unchangedReport.getTotalScore()).isNull();
    }

    @Test
    void failMarksReportFailed() throws Exception {
        Report report = saveGeneratingReport("APP-FAIL");

        mockMvc.perform(patch("/internal/reports/{reportId}/fail", report.getId())
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "errorMessage": "AI report generation failed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(report.getId()))
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        Report failedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(failedReport.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(failedReport.getReportKey()).isNull();
    }

    @Test
    void callbackRejectsMissingReport() throws Exception {
        mockMvc.perform(patch("/internal/reports/{reportId}/fail", 999999L)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void callbackRejectsMissingInternalApiKey() throws Exception {
        Report report = saveGeneratingReport("APP-NO-KEY");

        mockMvc.perform(patch("/internal/reports/{reportId}/fail", report.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        Report unchangedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(unchangedReport.getStatus()).isEqualTo(ReportStatus.GENERATING);
    }

    @Test
    void callbackRejectsInvalidInternalApiKey() throws Exception {
        Report report = saveGeneratingReport("APP-BAD-KEY");

        mockMvc.perform(patch("/internal/reports/{reportId}/fail", report.getId())
                        .header("X-Internal-Api-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        Report unchangedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(unchangedReport.getStatus()).isEqualTo(ReportStatus.GENERATING);
    }

    private Report saveGeneratingReport(String applicationNumber) {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Patent")
                .applicationNumber(applicationNumber)
                .build());

        return reportRepository.save(Report.builder()
                .patent(patent)
                .build());
    }
}
