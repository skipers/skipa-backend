package com.skipers.skipa.domain.patentextract.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.patent.application.PatentOriginalPdfStorageService;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patentextract.application.PatentExtractPublisher;
import com.skipers.skipa.domain.patentextract.application.PatentExtractStorageService;
import com.skipers.skipa.domain.patentextract.dao.PatentExtractJobRepository;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJobStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class PatentExtractAsyncFlowIntegrationTest {

    private static final String INTERNAL_API_KEY = "test-internal-api-key";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatentExtractJobRepository patentExtractJobRepository;

    @Autowired
    private PatentRepository patentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private PatentExtractStorageService patentExtractStorageService;

    @MockitoBean
    private PatentExtractPublisher patentExtractPublisher;

    @MockitoBean
    private PatentOriginalPdfStorageService patentOriginalPdfStorageService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void legalUserCompletesPatentExtractFlowAndCreatesPatentWithFinalPdfKey() throws Exception {
        String legalToken = createActiveUserToken("legal-patent-extract-flow", "legal-patent-extract-flow@example.com");
        when(patentExtractStorageService.generateUploadPresignedUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("https://minio.example.com/skipa/tmp/patent-extract-jobs/1/original.pdf?signature=abc");

        MvcResult uploadUrlResult = mockMvc.perform(post("/patent-extract-jobs/upload-url")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("UPLOAD_PENDING"))
                .andExpect(jsonPath("$.data.objectKey").isNotEmpty())
                .andExpect(jsonPath("$.data.uploadUrl").isNotEmpty())
                .andReturn();

        Long extractJobId = objectMapper.readTree(uploadUrlResult.getResponse().getContentAsString())
                .path("data")
                .path("extractJobId")
                .longValue();
        String objectKey = "tmp/patent-extract-jobs/%d/original.pdf".formatted(extractJobId);
        when(patentExtractStorageService.exists(objectKey)).thenReturn(true);

        mockMvc.perform(post("/patent-extract-jobs/{extractJobId}/upload-complete", extractJobId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractJobId").value(extractJobId))
                .andExpect(jsonPath("$.data.status").value("ANALYZING"));
        verify(patentExtractPublisher).publish(extractJobId, objectKey);

        mockMvc.perform(get("/patent-extract-jobs/{extractJobId}/result", extractJobId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PATENT_EXTRACT_NOT_COMPLETED"));

        mockMvc.perform(patch("/internal/patent-extract-jobs/{extractJobId}/complete", extractJobId)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "result": {
                                    "title": "AI Extracted Patent",
                                    "applicationNumber": "10-2026-0000000",
                                    "keywords": ["semiconductor", "package"],
                                    "summary": "AI generated summary"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractJobId").value(extractJobId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/patent-extract-jobs/{extractJobId}/status", extractJobId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        mockMvc.perform(get("/patent-extract-jobs/{extractJobId}/result", extractJobId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result.title").value("AI Extracted Patent"))
                .andExpect(jsonPath("$.data.result.keywords[0]").value("semiconductor"));

        mockMvc.perform(post("/patents")
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "AI Extracted Patent",
                                  "applicationNumber": "10-2026-0000000",
                                  "extractJobId": %d,
                                  "keywords": ["semiconductor", "package"],
                                  "summary": "AI generated summary"
                                }
                                """.formatted(extractJobId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.applicationNumber").value("10-2026-0000000"))
                .andExpect(jsonPath("$.data.originalPdfKey").value("patents/1/original.pdf"))
                .andExpect(jsonPath("$.data.parsedJsonKey").value("patents/1/parsed.json"));

        verify(patentOriginalPdfStorageService).copy(objectKey, "patents/1/original.pdf");
        verify(patentOriginalPdfStorageService).saveJson(
                org.mockito.ArgumentMatchers.eq("patents/1/parsed.json"),
                org.mockito.ArgumentMatchers.any(com.fasterxml.jackson.databind.JsonNode.class)
        );
        Patent patent = patentRepository.findByApplicationNumber("10-2026-0000000").orElseThrow();
        assertThat(patent.getOriginalPdfKey()).isEqualTo("patents/1/original.pdf");
        assertThat(patent.getParsedJsonKey()).isEqualTo("patents/1/parsed.json");
    }

    @Test
    void uploadCompleteRejectsMissingPdfWithoutPublishingMessage() throws Exception {
        String legalToken = createActiveUserToken("legal-patent-extract-missing-pdf", "legal-patent-extract-missing-pdf@example.com");
        PatentExtractJob job = saveUploadPendingJob(1L);
        when(patentExtractStorageService.exists(job.getObjectKey())).thenReturn(false);

        mockMvc.perform(post("/patent-extract-jobs/{extractJobId}/upload-complete", job.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_DOCUMENT_NOT_FOUND"));

        PatentExtractJob unchangedJob = patentExtractJobRepository.findById(job.getId()).orElseThrow();
        assertThat(unchangedJob.getStatus()).isEqualTo(PatentExtractJobStatus.UPLOAD_PENDING);
    }

    @Test
    void internalCallbackRejectsMissingApiKey() throws Exception {
        PatentExtractJob job = saveAnalyzingJob(1L);

        mockMvc.perform(patch("/internal/patent-extract-jobs/{extractJobId}/fail", job.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "errorMessage": "AI extraction failed"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        PatentExtractJob unchangedJob = patentExtractJobRepository.findById(job.getId()).orElseThrow();
        assertThat(unchangedJob.getStatus()).isEqualTo(PatentExtractJobStatus.ANALYZING);
    }

    @Test
    void aiFailureCallbackMarksJobFailedAndResultLookupRemainsUnavailable() throws Exception {
        String legalToken = createActiveUserToken("legal-patent-extract-fail", "legal-patent-extract-fail@example.com");
        PatentExtractJob job = saveAnalyzingJob(1L);

        mockMvc.perform(patch("/internal/patent-extract-jobs/{extractJobId}/fail", job.getId())
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "errorMessage": "AI extraction failed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractJobId").value(job.getId()))
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        mockMvc.perform(get("/patent-extract-jobs/{extractJobId}/status", job.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorMessage").value("AI extraction failed"));

        mockMvc.perform(get("/patent-extract-jobs/{extractJobId}/result", job.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PATENT_EXTRACT_NOT_COMPLETED"));
    }

    private PatentExtractJob saveUploadPendingJob(long seed) {
        PatentExtractJob job = patentExtractJobRepository.save(PatentExtractJob.createUploadPending());
        job.assignObjectKey("tmp/patent-extract-jobs/%d/original.pdf".formatted(seed));
        return job;
    }

    private PatentExtractJob saveAnalyzingJob(long seed) {
        PatentExtractJob job = saveUploadPendingJob(seed);
        job.markUploadCompleted(null);
        return job;
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
