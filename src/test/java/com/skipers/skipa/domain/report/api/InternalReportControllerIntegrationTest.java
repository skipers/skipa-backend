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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportKey": "reports/%d/report.html"
                                }
                                """.formatted(report.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(report.getId()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        Report completedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(completedReport.getStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(completedReport.getReportKey()).isEqualTo("reports/%d/report.html".formatted(report.getId()));
        assertThat(completedReport.getEvaluatedAt()).isNotNull();
    }

    @Test
    void completeRejectsBlankReportKey() throws Exception {
        Report report = saveGeneratingReport("APP-BLANK");

        mockMvc.perform(patch("/internal/reports/{reportId}/complete", report.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportKey": " "
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
    void failMarksReportFailed() throws Exception {
        Report report = saveGeneratingReport("APP-FAIL");

        mockMvc.perform(patch("/internal/reports/{reportId}/fail", report.getId())
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_FOUND"));
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
