package com.skipers.skipa.domain.report.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.report.application.ReportGenerationPublisher;
import com.skipers.skipa.domain.report.application.ReportStorageService;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ReportAsyncFlowIntegrationTest {

    private static final String INTERNAL_API_KEY = "test-internal-api-key";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatentRepository patentRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private ReportGenerationPublisher reportGenerationPublisher;

    @MockitoBean
    private ReportStorageService reportStorageService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void legalUserCreatesReportAndAiCallbackCompletesAsyncFlow() throws Exception {
        Patent patent = savePatent("APP-ASYNC-COMPLETE");
        String legalToken = createActiveUserToken("legal-report-flow", "legal-report-flow@example.com");

        MvcResult createResult = mockMvc.perform(post("/patents/{patentId}/reports", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.patentId").value(patent.getId()))
                .andExpect(jsonPath("$.data.status").value("GENERATING"))
                .andReturn();

        Long reportId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .longValue();
        verify(reportGenerationPublisher).publish(reportId, patent.getId());

        mockMvc.perform(get("/patents/{patentId}/reports/{reportId}/status", patent.getId(), reportId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GENERATING"))
                .andExpect(jsonPath("$.data.evaluatedAt").isEmpty());

        mockMvc.perform(get("/patents/{patentId}/reports/latest", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reportId))
                .andExpect(jsonPath("$.data.status").value("GENERATING"))
                .andExpect(jsonPath("$.data.url").isEmpty());

        mockMvc.perform(get("/patents/{patentId}/reports/{reportId}", patent.getId(), reportId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_COMPLETED"));

        mockMvc.perform(patch("/internal/reports/{reportId}/complete", reportId)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportKey": "reports/%d/report.html"
                                }
                                """.formatted(reportId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(reportId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        when(reportStorageService.generatePresignedUrl("reports/%d/report.html".formatted(reportId)))
                .thenReturn("https://minio.example.com/reports/%d/report.html?signature=abc".formatted(reportId));

        mockMvc.perform(get("/patents/{patentId}/reports/{reportId}/status", patent.getId(), reportId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.evaluatedAt").isNotEmpty());

        mockMvc.perform(get("/patents/{patentId}/reports/{reportId}", patent.getId(), reportId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.url").value("https://minio.example.com/reports/%d/report.html?signature=abc".formatted(reportId)))
                .andExpect(jsonPath("$.data.reportKey").doesNotExist());

        mockMvc.perform(get("/patents/{patentId}/reports/latest", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reportId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.url").value("https://minio.example.com/reports/%d/report.html?signature=abc".formatted(reportId)))
                .andExpect(jsonPath("$.data.reportKey").doesNotExist());
    }

    @Test
    void aiFailureCallbackMarksReportFailedAndDetailLookupRemainsUnavailable() throws Exception {
        Patent patent = savePatent("APP-ASYNC-FAIL");
        Report report = reportRepository.save(Report.builder()
                .patent(patent)
                .build());
        String legalToken = createActiveUserToken("legal-report-fail", "legal-report-fail@example.com");

        mockMvc.perform(patch("/internal/reports/{reportId}/fail", report.getId())
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "errorMessage": "AI report generation failed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        Report failedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(failedReport.getStatus()).isEqualTo(ReportStatus.FAILED);

        mockMvc.perform(get("/patents/{patentId}/reports/{reportId}/status", patent.getId(), report.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        mockMvc.perform(get("/patents/{patentId}/reports/{reportId}", patent.getId(), report.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_COMPLETED"));

        mockMvc.perform(get("/patents/{patentId}/reports/latest", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(report.getId()))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.url").isEmpty());
    }

    @Test
    void reportCreateReturnsExternalServiceErrorWhenRabbitPublisherFails() throws Exception {
        Patent patent = savePatent("APP-PUBLISH-FAIL");
        String legalToken = createActiveUserToken("legal-publish-fail", "legal-publish-fail@example.com");
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(reportGenerationPublisher)
                .publish(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(patent.getId()));

        mockMvc.perform(post("/patents/{patentId}/reports", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("EXTERNAL_SERVICE_ERROR"));
    }

    @Test
    void latestReportReturnsNotFoundWhenPatentHasNoReport() throws Exception {
        Patent patent = savePatent("APP-LATEST-NONE");
        String legalToken = createActiveUserToken("legal-latest-none", "legal-latest-none@example.com");

        mockMvc.perform(get("/patents/{patentId}/reports/latest", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_FOUND"));
    }

    private Patent savePatent(String applicationNumber) {
        return patentRepository.save(Patent.builder()
                .title("Report Flow Patent")
                .applicationNumber(applicationNumber)
                .build());
    }

    private String createActiveUserToken(String loginId, String email) {
        User user = userRepository.save(User.createActive(
                loginId,
                "Legal User",
                email,
                passwordEncoder.encode("password"),
                UserRole.LEGAL,
                null
        ));

        return jwtProvider.createAccessToken(user.getId(), UserRole.LEGAL);
    }
}
