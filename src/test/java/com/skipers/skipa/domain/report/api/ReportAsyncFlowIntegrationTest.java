package com.skipers.skipa.domain.report.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.dao.ChatMessageRepository;
import com.skipers.skipa.domain.chat.domain.ChatRole;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
import com.skipers.skipa.domain.chat.dto.ChatClientResult;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.report.application.ReportChatClient;
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

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private ReportGenerationPublisher reportGenerationPublisher;

    @MockitoBean
    private ReportStorageService reportStorageService;

    @MockitoBean
    private ReportChatClient reportChatClient;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void legalUserCreatesReportAndAiCallbackCompletesAsyncFlow() throws Exception {
        Patent patent = savePatent("APP-ASYNC-COMPLETE");
        Report oldReport = reportRepository.save(Report.builder()
                .patent(patent)
                .build());
        oldReport.completeReport(
                "reports/%d/report.html".formatted(oldReport.getId()),
                new BigDecimal("91.00"),
                "S",
                Instant.parse("2026-01-01T00:00:00Z")
        );
        String legalToken = createActiveUserToken("legal-report-flow", "legal-report-flow@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/v1/patents/{patentId}/reports", patent.getId())
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

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}/status", patent.getId(), reportId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GENERATING"))
                .andExpect(jsonPath("$.data.evaluatedAt").isEmpty());

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/latest", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reportId))
                .andExpect(jsonPath("$.data.status").value("GENERATING"))
                .andExpect(jsonPath("$.data.url").isEmpty());

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}", patent.getId(), reportId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_COMPLETED"));

        mockMvc.perform(patch("/api/v1/internal/reports/{reportId}/complete", reportId)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportKey": "reports/%d/report.html",
                                  "totalScore": 82.5,
                                  "valueGrade": "A"
                                }
                                """.formatted(reportId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(reportId))
                .andExpect(jsonPath("$.data.status").value("REPORT_COMPLETED"))
                .andExpect(jsonPath("$.data.totalScore").value(82.5))
                .andExpect(jsonPath("$.data.valueGrade").value("A"));

        when(reportStorageService.generatePresignedUrl("reports/%d/report.html".formatted(reportId)))
                .thenReturn("https://minio.example.com/reports/%d/report.html?signature=abc".formatted(reportId));

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}/status", patent.getId(), reportId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REPORT_COMPLETED"))
                .andExpect(jsonPath("$.data.totalScore").value(82.5))
                .andExpect(jsonPath("$.data.valueGrade").value("A"))
                .andExpect(jsonPath("$.data.evaluatedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}", patent.getId(), reportId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REPORT_COMPLETED"))
                .andExpect(jsonPath("$.data.totalScore").value(82.5))
                .andExpect(jsonPath("$.data.valueGrade").value("A"))
                .andExpect(jsonPath("$.data.url").value("https://minio.example.com/reports/%d/report.html?signature=abc".formatted(reportId)))
                .andExpect(jsonPath("$.data.reportKey").doesNotExist());

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/latest", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reportId))
                .andExpect(jsonPath("$.data.status").value("REPORT_COMPLETED"))
                .andExpect(jsonPath("$.data.totalScore").value(82.5))
                .andExpect(jsonPath("$.data.valueGrade").value("A"))
                .andExpect(jsonPath("$.data.url").value("https://minio.example.com/reports/%d/report.html?signature=abc".formatted(reportId)))
                .andExpect(jsonPath("$.data.reportKey").doesNotExist());

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/history", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(oldReport.getId()))
                .andExpect(jsonPath("$.data.items[0].patentId").value(patent.getId()))
                .andExpect(jsonPath("$.data.items[0].totalScore").value(91.0))
                .andExpect(jsonPath("$.data.items[0].valueGrade").value("S"))
                .andExpect(jsonPath("$.data.items[0].evaluatedAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.items[0].opinion").isEmpty())
                .andExpect(jsonPath("$.data.items[0].comment").isEmpty());
    }

    @Test
    void aiFailureCallbackMarksReportFailedAndDetailLookupRemainsUnavailable() throws Exception {
        Patent patent = savePatent("APP-ASYNC-FAIL");
        Report report = reportRepository.save(Report.builder()
                .patent(patent)
                .build());
        String legalToken = createActiveUserToken("legal-report-fail", "legal-report-fail@example.com");

        mockMvc.perform(patch("/api/v1/internal/reports/{reportId}/fail", report.getId())
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

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}/status", patent.getId(), report.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}", patent.getId(), report.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_COMPLETED"));

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/latest", patent.getId())
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

        mockMvc.perform(post("/api/v1/patents/{patentId}/reports", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("EXTERNAL_SERVICE_ERROR"));
    }

    @Test
    void latestReportReturnsNotFoundWhenPatentHasNoReport() throws Exception {
        Patent patent = savePatent("APP-LATEST-NONE");
        String legalToken = createActiveUserToken("legal-latest-none", "legal-latest-none@example.com");

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/latest", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void legalUserChatsWithCompletedReportAndClearsMessages() throws Exception {
        Patent patent = savePatent("APP-REPORT-CHAT");
        Report report = reportRepository.save(Report.builder()
                .patent(patent)
                .build());
        report.completeReport("reports/%d/report.json".formatted(report.getId()), new BigDecimal("82.50"), "A", null);
        String legalToken = createActiveUserToken("legal-report-chat", "legal-report-chat@example.com");
        when(reportChatClient.send(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ChatClientResult.answerOnly("The strongest risk is claim breadth."));

        mockMvc.perform(post("/api/v1/patents/{patentId}/reports/{reportId}/chat/messages", patent.getId(), report.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "What is the key risk?"
                                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userMessage.patentId").value(patent.getId()))
                .andExpect(jsonPath("$.data.userMessage.role").value("USER"))
                .andExpect(jsonPath("$.data.userMessage.content").value("What is the key risk?"))
                .andExpect(jsonPath("$.data.assistantMessage.patentId").value(patent.getId()))
                .andExpect(jsonPath("$.data.assistantMessage.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.assistantMessage.content").value("The strongest risk is claim breadth."));

        assertThat(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.REPORT, patent.getId()))
                .extracting(message -> message.getRole())
                .containsExactly(ChatRole.USER, ChatRole.ASSISTANT);

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}/chat/messages", patent.getId(), report.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[1].role").value("ASSISTANT"));

        mockMvc.perform(delete("/api/v1/patents/{patentId}/reports/{reportId}/chat/messages", patent.getId(), report.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk());

        assertThat(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.REPORT, patent.getId()))
                .isEmpty();
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
