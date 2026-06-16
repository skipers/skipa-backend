package com.skipers.skipa.domain.preevaluation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.dao.ChatMessageRepository;
import com.skipers.skipa.domain.chat.domain.ChatRole;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
import com.skipers.skipa.domain.chat.dto.ChatClientResult;
import com.skipers.skipa.domain.preevaluation.application.PreEvaluationChatClient;
import com.skipers.skipa.domain.preevaluation.application.PreEvaluationGenerationPublisher;
import com.skipers.skipa.domain.preevaluation.dao.PreEvaluationRepository;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluationStatus;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatClientRequest;
import com.skipers.skipa.domain.report.application.ReportStorageService;
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
class PreEvaluationFlowIntegrationTest {

    private static final String INTERNAL_API_KEY = "test-internal-api-key";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PreEvaluationRepository preEvaluationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private PreEvaluationGenerationPublisher generationPublisher;

    @MockitoBean
    private PreEvaluationChatClient chatClient;

    @MockitoBean
    private ReportStorageService reportStorageService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void businessUserCreatesPreEvaluationAndAiCallbackCompletesFlow() throws Exception {
        User businessUser = saveActiveUser("business-pre-evaluation", "business-pre-evaluation@example.com", UserRole.BUSINESS);
        String businessToken = jwtProvider.createAccessToken(businessUser.getId(), UserRole.BUSINESS);

        MvcResult createResult = mockMvc.perform(post("/api/v1/pre-evaluations")
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Battery safety system",
                                  "technicalDescription": "Detects battery thermal runaway early.",
                                  "claims": [
                                    "A battery safety system comprising a sensor unit.",
                                    "The system of claim 1, further comprising a warning unit."
                                  ],
                                  "relatedBusiness": "EV battery",
                                  "targetCountries": "Korea, United States"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(businessUser.getId()))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andReturn();

        Long preEvaluationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .longValue();
        PreEvaluation created = preEvaluationRepository.findById(preEvaluationId).orElseThrow();
        assertThat(created.getClaims())
                .containsExactly(
                        "A battery safety system comprising a sensor unit.",
                        "The system of claim 1, further comprising a warning unit."
                );
        verify(generationPublisher).publish(created);

        mockMvc.perform(get("/api/v1/pre-evaluations/{preEvaluationId}/status", preEvaluationId)
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.reportUrl").doesNotExist());

        mockMvc.perform(patch("/api/v1/internal/pre-evaluations/{preEvaluationId}/complete", preEvaluationId)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportKey": "pre-evaluations/%d/report.json"
                                }
                                """.formatted(preEvaluationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preEvaluationId").value(preEvaluationId))
                .andExpect(jsonPath("$.data.status").value("REPORT_COMPLETED"))
                .andExpect(jsonPath("$.data.reportUrl").doesNotExist());

        PreEvaluation completed = preEvaluationRepository.findById(preEvaluationId).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(PreEvaluationStatus.REPORT_COMPLETED);
        assertThat(completed.getReportKey()).isEqualTo("pre-evaluations/%d/report.json".formatted(preEvaluationId));
        assertThat(completed.getCompletedAt()).isNotNull();

        mockMvc.perform(patch("/api/v1/internal/pre-evaluations/{preEvaluationId}/embedding-complete", preEvaluationId)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preEvaluationId").value(preEvaluationId))
                .andExpect(jsonPath("$.data.status").value("EMBEDDING_COMPLETED"));

        completed = preEvaluationRepository.findById(preEvaluationId).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(PreEvaluationStatus.EMBEDDING_COMPLETED);

        when(reportStorageService.generatePresignedUrl("pre-evaluations/%d/report.json".formatted(preEvaluationId)))
                .thenReturn("https://minio.example.com/presigned/pre-evaluations/%d/report.json".formatted(preEvaluationId));

        mockMvc.perform(get("/api/v1/pre-evaluations/{preEvaluationId}", preEvaluationId)
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(preEvaluationId))
                .andExpect(jsonPath("$.data.claims.length()").value(2))
                .andExpect(jsonPath("$.data.status").value("EMBEDDING_COMPLETED"))
                .andExpect(jsonPath("$.data.reportUrl").value("https://minio.example.com/presigned/pre-evaluations/%d/report.json".formatted(preEvaluationId)));
    }

    @Test
    void preEvaluationApisAllowOnlyBusinessUsers() throws Exception {
        String legalToken = jwtProvider.createAccessToken(
                saveActiveUser("legal-pre-evaluation", "legal-pre-evaluation@example.com", UserRole.LEGAL).getId(),
                UserRole.LEGAL
        );
        String adminToken = jwtProvider.createAccessToken(
                saveActiveUser("admin-pre-evaluation", "admin-pre-evaluation@example.com", UserRole.ADMIN).getId(),
                UserRole.ADMIN
        );

        mockMvc.perform(post("/api/v1/pre-evaluations")
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/pre-evaluations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void businessUserChatsWithPreEvaluationAndClearsMessages() throws Exception {
        User businessUser = saveActiveUser("business-chat", "business-chat@example.com", UserRole.BUSINESS);
        String businessToken = jwtProvider.createAccessToken(businessUser.getId(), UserRole.BUSINESS);
        PreEvaluation preEvaluation = preEvaluationRepository.save(PreEvaluation.builder()
                .user(businessUser)
                .title("Battery safety system")
                .technicalDescription("Detects battery thermal runaway early.")
                .claims(java.util.List.of("A battery safety system comprising a sensor unit."))
                .relatedBusiness("EV battery")
                .targetCountries("Korea, United States")
                .build());
        when(chatClient.send(org.mockito.ArgumentMatchers.any(PreEvaluationChatClientRequest.class)))
                .thenReturn(ChatClientResult.answerOnly("Strengthen the claim around the detection algorithm."));

        mockMvc.perform(post("/api/v1/pre-evaluations/{preEvaluationId}/chat/messages", preEvaluation.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "How can I improve this patent?"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userMessage.role").value("USER"))
                .andExpect(jsonPath("$.data.userMessage.content").value("How can I improve this patent?"))
                .andExpect(jsonPath("$.data.assistantMessage.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.assistantMessage.content").value("Strengthen the claim around the detection algorithm."));

        assertThat(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                        ChatTargetType.PRE_EVALUATION,
                        preEvaluation.getId()
                ))
                .extracting(message -> message.getRole())
                .containsExactly(ChatRole.USER, ChatRole.ASSISTANT);

        mockMvc.perform(get("/api/v1/pre-evaluations/{preEvaluationId}/chat/messages", preEvaluation.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[1].role").value("ASSISTANT"));

        mockMvc.perform(delete("/api/v1/pre-evaluations/{preEvaluationId}/chat/messages", preEvaluation.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk());

        assertThat(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ChatTargetType.PRE_EVALUATION,
                preEvaluation.getId()
        )).isEmpty();
    }

    @Test
    void deletingPreEvaluationRemovesChatMessages() throws Exception {
        User businessUser = saveActiveUser("business-delete", "business-delete@example.com", UserRole.BUSINESS);
        String businessToken = jwtProvider.createAccessToken(businessUser.getId(), UserRole.BUSINESS);
        PreEvaluation preEvaluation = preEvaluationRepository.save(PreEvaluation.builder()
                .user(businessUser)
                .title("Battery safety system")
                .technicalDescription("Detects battery thermal runaway early.")
                .claims(java.util.List.of("A battery safety system comprising a sensor unit."))
                .build());
        when(chatClient.send(org.mockito.ArgumentMatchers.any(PreEvaluationChatClientRequest.class)))
                .thenReturn(ChatClientResult.answerOnly("AI answer"));
        mockMvc.perform(post("/api/v1/pre-evaluations/{preEvaluationId}/chat/messages", preEvaluation.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "question"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/pre-evaluations/{preEvaluationId}", preEvaluation.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk());

        assertThat(preEvaluationRepository.findById(preEvaluation.getId())).isEmpty();
        assertThat(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ChatTargetType.PRE_EVALUATION,
                preEvaluation.getId()
        )).isEmpty();
    }

    @Test
    void internalCallbackRejectsMissingApiKey() throws Exception {
        User businessUser = saveActiveUser("business-internal", "business-internal@example.com", UserRole.BUSINESS);
        PreEvaluation preEvaluation = preEvaluationRepository.save(PreEvaluation.builder()
                .user(businessUser)
                .title("Battery safety system")
                .technicalDescription("Detects battery thermal runaway early.")
                .claims(java.util.List.of("A battery safety system comprising a sensor unit."))
                .build());

        mockMvc.perform(patch("/api/v1/internal/pre-evaluations/{preEvaluationId}/fail", preEvaluation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        assertThat(preEvaluationRepository.findById(preEvaluation.getId()).orElseThrow().getStatus())
                .isEqualTo(PreEvaluationStatus.PROCESSING);
    }

    private User saveActiveUser(String loginId, String email, UserRole role) {
        return userRepository.save(User.createActive(
                loginId,
                "User",
                email,
                passwordEncoder.encode("password"),
                role,
                null
        ));
    }

    private String validCreateBody() {
        return """
                {
                  "title": "Battery safety system",
                  "technicalDescription": "Detects battery thermal runaway early.",
                  "claims": [
                    "A battery safety system comprising a sensor unit."
                  ],
                  "relatedBusiness": "EV battery",
                  "targetCountries": "Korea, United States"
                }
                """;
    }
}
