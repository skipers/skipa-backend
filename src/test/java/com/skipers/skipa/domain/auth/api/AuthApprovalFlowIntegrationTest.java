package com.skipers.skipa.domain.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentApprovalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.application.ReportGenerationPublisher;
import com.skipers.skipa.domain.report.application.ReportStorageService;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.domain.user.domain.UserStatus;
import com.skipers.skipa.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class AuthApprovalFlowIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PatentRepository patentRepository;

    @Autowired
    private PatentLegalStatusRepository patentLegalStatusRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewCycleRepository reviewCycleRepository;

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

    private Department department;
    private ReviewCycle reviewCycle;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        department = departmentRepository.save(Department.builder()
                .name("통신")
                .build());
        reviewCycle = reviewCycleRepository.save(ReviewCycle.builder()
                .year(2026)
                .quarter(2)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .deadline(LocalDate.now().plusDays(1))
                .build());

        userRepository.save(User.createActive(
                "admin",
                "Administrator",
                "admin@example.com",
                passwordEncoder.encode("admin-password"),
                UserRole.ADMIN,
                null
        ));
    }

    @Test
    void registeredUserRemainsPendingAndCannotLogInOrAccessProtectedApi() throws Exception {
        Long userId = registerBusinessUser();
        User pendingUser = userRepository.findById(userId).orElseThrow();

        assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(pendingUser.getDepartment()).isNull();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "business-new",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PENDING_USER"));

        String pendingToken = jwtProvider.createAccessToken(userId, UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + pendingToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PENDING_USER"));
    }

    @Test
    void approvedBusinessUserBecomesActiveAndCanLogInButCannotReadDepartments() throws Exception {
        Long userId = registerBusinessUser();
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/approve", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": %d
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.departmentId").value(department.getId()));

        User approvedUser = userRepository.findById(userId).orElseThrow();
        assertThat(approvedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(approvedUser.getDepartment().getId()).isEqualTo(department.getId());

        String userToken = loginAndGetAccessToken("business-new", "password");

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void approvedLegalUserBecomesActiveWithoutDepartment() throws Exception {
        MvcResult registrationResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "legal-new",
                                  "password": "password",
                                  "name": "Legal User",
                                  "email": "legal-new@example.com",
                                  "role": "LEGAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        Long userId = objectMapper.readTree(registrationResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .longValue();
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/approve", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.departmentId").value(nullValue()));

        User approvedUser = userRepository.findById(userId).orElseThrow();
        assertThat(approvedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(approvedUser.getDepartment()).isNull();

        loginAndGetAccessToken("legal-new", "password");
    }

    @Test
    void commonAuthApisReturnMeRefreshAccessTokenAndLogout() throws Exception {
        Long userId = registerBusinessUser();
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/approve", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": %d
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "business-new",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loginData = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data");
        String accessToken = loginData.path("accessToken").textValue();
        String refreshToken = loginData.path("refreshToken").textValue();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(userId))
                .andExpect(jsonPath("$.data.user.loginId").value("business-new"))
                .andExpect(jsonPath("$.data.user.role").value("BUSINESS"))
                .andExpect(jsonPath("$.data.user.departmentId").value(department.getId()))
                .andExpect(jsonPath("$.data.user.departmentName").value(department.getName()));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void swaggerConfigAndOpenApiDocsRemainUnversioned() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void meReturnsBusinessDepartmentOutsideTestTransaction() throws Exception {
        Department savedDepartment = departmentRepository.save(Department.builder()
                .name("Auth Me Department")
                .build());
        User businessUser = userRepository.save(User.createActive(
                "business-me-detached",
                "Business Me",
                "business-me-detached@example.com",
                passwordEncoder.encode("password"),
                UserRole.BUSINESS,
                savedDepartment
        ));
        String businessToken = jwtProvider.createAccessToken(businessUser.getId(), UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(businessUser.getId()))
                .andExpect(jsonPath("$.data.user.role").value("BUSINESS"))
                .andExpect(jsonPath("$.data.user.departmentId").value(savedDepartment.getId()))
                .andExpect(jsonPath("$.data.user.departmentName").value("Auth Me Department"));
    }

    @Test
    void approvalRequiresAdminAndReturnsRequestAndDomainErrors() throws Exception {
        Long userId = registerBusinessUser();
        String legalToken = createActiveUserToken("legal-approver", "legal-approver@example.com", UserRole.LEGAL);
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/approve", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": %d
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/approve", userId)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": %d
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/approve", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/approve", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": 999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEPARTMENT_NOT_FOUND"));

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/approve", 999999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": %d
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    void departmentReadApisAllowAdminAndLegalButRejectBusinessAndUnauthenticatedUser() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "admin-password");
        String legalToken = createActiveUserToken("legal-active", "legal-active@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-active", "business-active@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void departmentWriteApisAllowAdminAndLegalButRejectBusinessAndUnauthenticatedUser() throws Exception {
        String legalToken = createActiveUserToken("legal-active", "legal-active@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-active", "business-active@example.com", UserRole.BUSINESS);
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unauthenticated Create"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/departments/{departmentId}", department.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unauthenticated Update"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/departments/{departmentId}", department.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Business Create"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Business Update"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        MvcResult createResult = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Department"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("New Department"))
                .andReturn();

        Long createdDepartmentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .longValue();

        mockMvc.perform(put("/api/v1/departments/{departmentId}", createdDepartmentId)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Department"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Department"));

        mockMvc.perform(delete("/api/v1/departments/{departmentId}", createdDepartmentId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == %d)]".formatted(createdDepartmentId)).isEmpty());

        mockMvc.perform(get("/api/v1/departments/{departmentId}", createdDepartmentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void assignedDepartmentCanBeDeactivatedWithoutRemovingReferences() throws Exception {
        createActiveUserToken("assigned-business", "assigned-business@example.com", UserRole.BUSINESS);
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(delete("/api/v1/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(departmentRepository.existsById(department.getId())).isTrue();
        assertThat(departmentRepository.findById(department.getId()).orElseThrow().getStatus().name()).isEqualTo("INACTIVE");
    }

    @Test
    void departmentApisReturnDuplicateAndNotFoundErrors() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "통신"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_DEPARTMENT_NAME"));

        mockMvc.perform(get("/api/v1/departments/{departmentId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEPARTMENT_NOT_FOUND"));

        mockMvc.perform(put("/api/v1/departments/{departmentId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unknown"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEPARTMENT_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/departments/{departmentId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    void departmentSearchUsesKeywordAndPagingAgainstDatabase() throws Exception {
        departmentRepository.save(Department.builder().name("Legal Affairs").build());
        departmentRepository.save(Department.builder().name("Legal Operations").build());
        String legalToken = createActiveUserToken("legal-reader", "legal-reader@example.com", UserRole.LEGAL);

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("keyword", "legal")
                        .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void patentApisAllowAdminAndLegalManagementAndReturnDecodedLists() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "admin-password");
        String legalToken = createActiveUserToken("legal-patent", "legal-patent@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-patent", "business-patent@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/patents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/patents")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));

        mockMvc.perform(post("/api/v1/patents")
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "title": "Business Patent",
                  "applicationNumber": "BUSINESS-1"
                }
                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/patents/{patentId}", 1L)
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Business Update",
                                  "applicationNumber": "BUSINESS-2"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/patents/{patentId}", 1L)
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        MvcResult createResult = mockMvc.perform(post("/api/v1/patents")
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Chip Patent  ",
                                  "applicationNumber": " APP-1 ",
                                  "applicant": " Search Applicant ",
                                  "inventor": " Search Inventor ",
                                  "ipcCodes": [" H01L 21/00 "],
                                  "cpcCodes": [" H01L 23/00 "],
                                  "relatedProducts": [" Product "],
                                  "initialDepartment": " Initial Legal ",
                                  "keywords": [" Keyword "]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("  Chip Patent  "))
                .andExpect(jsonPath("$.data.applicationNumber").value(" APP-1 "))
                .andExpect(jsonPath("$.data.applicant").value(" Search Applicant "))
                .andExpect(jsonPath("$.data.inventor").value(" Search Inventor "))
                .andExpect(jsonPath("$.data.ipcCodes[0]").value(" H01L 21/00 "))
                .andExpect(jsonPath("$.data.cpcCodes[0]").value(" H01L 23/00 "))
                .andExpect(jsonPath("$.data.relatedProducts[0]").value(" Product "))
                .andExpect(jsonPath("$.data.initialDepartment").value(" Initial Legal "))
                .andExpect(jsonPath("$.data.keywords[0]").value(" Keyword "))
                .andReturn();

        Long patentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .longValue();

        mockMvc.perform(get("/api/v1/patents/{patentId}", patentId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ipcCodes[0]").value(" H01L 21/00 "))
                .andExpect(jsonPath("$.data.cpcCodes[0]").value(" H01L 23/00 "))
                .andExpect(jsonPath("$.data.relatedProducts[0]").value(" Product "))
                .andExpect(jsonPath("$.data.keywords[0]").value(" Keyword "))
                .andExpect(jsonPath("$.data.latestLegalStatus").value(nullValue()));

        mockMvc.perform(get("/api/v1/patents")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", " chip ")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("  Chip Patent  "))
                .andExpect(jsonPath("$.data.items[0].ipcCodes[0]").value(" H01L 21/00 "))
                .andExpect(jsonPath("$.data.items[0].cpcCodes[0]").value(" H01L 23/00 "));

        mockMvc.perform(get("/api/v1/patents")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "APP-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(patentId));

        mockMvc.perform(get("/api/v1/patents")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "inventor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(patentId));

        mockMvc.perform(get("/api/v1/patents")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "applicant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(patentId));

        mockMvc.perform(put("/api/v1/patents/{patentId}", patentId)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Patent",
                                  "applicationNumber": "APP-UPDATED",
                                  "ipcCodes": ["Updated IPC"],
                                  "cpcCodes": ["Updated CPC"],
                                  "relatedProducts": ["Updated Product"],
                                  "initialDepartment": "Updated Legal",
                                  "keywords": ["Updated Keyword"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Patent"))
                .andExpect(jsonPath("$.data.ipcCodes[0]").value("Updated IPC"))
                .andExpect(jsonPath("$.data.cpcCodes[0]").value("Updated CPC"))
                .andExpect(jsonPath("$.data.relatedProducts[0]").value("Updated Product"))
                .andExpect(jsonPath("$.data.initialDepartment").value("Updated Legal"));

        mockMvc.perform(delete("/api/v1/patents/{patentId}", patentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(patentRepository.existsById(patentId)).isFalse();
    }

    @Test
    void legalUserCanReadPendingApprovalPatents() throws Exception {
        Patent pendingPatent = patentRepository.save(Patent.builder()
                .title("Pending Approval Patent")
                .applicationNumber("APP-PENDING-APPROVAL")
                .approvalStatus(PatentApprovalStatus.PENDING_APPROVAL)
                .currentDepartment(department)
                .build());
        Patent approvedPatent = patentRepository.save(Patent.builder()
                .title("Approved Patent")
                .applicationNumber("APP-APPROVED")
                .approvalStatus(PatentApprovalStatus.APPROVED)
                .build());
        patentRepository.save(Patent.builder()
                .title("Rejected Patent")
                .applicationNumber("APP-REJECTED")
                .approvalStatus(PatentApprovalStatus.REJECTED)
                .build());
        String legalToken = createActiveUserToken("legal-pending-patent", "legal-pending-patent@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-pending-patent", "business-pending-patent@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/patents/pending-approval"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/patents/pending-approval")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/patents/pending-approval")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("keyword", "pending")
                        .param("sort", "applicationNumber,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(pendingPatent.getId()))
                .andExpect(jsonPath("$.data.items[0].approvalStatus").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.totalItems").value(1));

        mockMvc.perform(get("/api/v1/patents")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("approvalStatus", "PENDING_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(approvedPatent.getId()))
                .andExpect(jsonPath("$.data.items[0].approvalStatus").value("APPROVED"));
    }

    @Test
    void patentSubresourcesRejectPendingApprovalPatents() throws Exception {
        Patent pendingPatent = patentRepository.save(Patent.builder()
                .title("Pending Subresource Patent")
                .applicationNumber("APP-PENDING-SUB")
                .approvalStatus(PatentApprovalStatus.PENDING_APPROVAL)
                .currentDepartment(department)
                .build());
        String legalToken = createActiveUserToken("legal-pending-subresource", "legal-pending-subresource@example.com", UserRole.LEGAL);

        mockMvc.perform(post("/api/v1/patents/{patentId}/reports", pendingPatent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports", pendingPatent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/patents/{patentId}/legal-status", pendingPatent.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REGISTERED",
                                  "changedAt": "2026-06-10"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/patents/{patentId}/legal-status", pendingPatent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/patents/{patentId}/annuities", pendingPatent.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentYears": 1,
                                  "amount": 100000
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/patents/{patentId}/annuities", pendingPatent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/patents/{patentId}/reviews", pendingPatent.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));
    }

    @Test
    void businessUserCanListAllPatentsAndListOnlyCurrentlyAssignedPatentsSeparately() throws Exception {
        Department otherDepartment = departmentRepository.save(Department.builder()
                .name("제조")
                .build());
        Patent assignedPatent = patentRepository.save(Patent.builder()
                .title("Assigned Patent")
                .applicationNumber("APP-CURRENT-DEPT")
                .currentDepartment(department)
                .build());
        Patent otherPatent = patentRepository.save(Patent.builder()
                .title("Other Patent")
                .applicationNumber("APP-OTHER-DEPT")
                .techField("Semiconductor")
                .businessField("Memory")
                .currentDepartment(otherDepartment)
                .build());
        String businessToken = createActiveUserToken("business-patent-reader", "business-patent-reader@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/patents")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(assignedPatent.getId()));

        mockMvc.perform(get("/api/v1/patents")
                        .header("Authorization", "Bearer " + businessToken)
                        .param("keyword", "assigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(assignedPatent.getId()));

        mockMvc.perform(get("/api/v1/patents/assigned")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(assignedPatent.getId()));

        mockMvc.perform(get("/api/v1/patents/{patentId}", assignedPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assignedPatent.getId()));

        mockMvc.perform(get("/api/v1/patents/{patentId}", otherPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(otherPatent.getId()))
                .andExpect(jsonPath("$.data.title").value("Other Patent"))
                .andExpect(jsonPath("$.data.techField").value("Semiconductor"))
                .andExpect(jsonPath("$.data.businessField").value("Memory"))
                .andExpect(jsonPath("$.data.latestReportScore").value(nullValue()));
    }

    @Test
    void patentSummaryCountsAllPatentsForLegalAndDepartmentPatentsForBusiness() throws Exception {
        Department otherDepartment = departmentRepository.save(Department.builder()
                .name("제조")
                .build());
        Patent ownMaintainPatent = patentRepository.save(Patent.builder()
                .title("Own Maintain Patent")
                .applicationNumber("SUM-OWN-MAIN")
                .currentDepartment(department)
                .build());
        Patent ownAbandonPatent = patentRepository.save(Patent.builder()
                .title("Own Abandon Patent")
                .applicationNumber("SUM-OWN-ABAN")
                .currentDepartment(department)
                .build());
        Patent otherMaintainPatent = patentRepository.save(Patent.builder()
                .title("Other Maintain Patent")
                .applicationNumber("SUM-OTHER-MAIN")
                .currentDepartment(otherDepartment)
                .build());
        Patent noStatusPatent = patentRepository.save(Patent.builder()
                .title("No Status Patent")
                .applicationNumber("SUM-NO-STATUS")
                .currentDepartment(otherDepartment)
                .build());
        patentLegalStatusRepository.save(PatentLegalStatus.builder()
                .patent(ownMaintainPatent)
                .status(PatentLegalStatusType.REGISTERED)
                .changedAt(LocalDate.now())
                .build());
        patentLegalStatusRepository.save(PatentLegalStatus.builder()
                .patent(ownAbandonPatent)
                .status(PatentLegalStatusType.EXPIRED)
                .changedAt(LocalDate.now())
                .build());
        patentLegalStatusRepository.save(PatentLegalStatus.builder()
                .patent(otherMaintainPatent)
                .status(PatentLegalStatusType.APPLIED)
                .changedAt(LocalDate.now())
                .build());
        String legalToken = createActiveUserToken("legal-patent-summary", "legal-patent-summary@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-patent-summary", "business-patent-summary@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/patents/summary")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(2))
                .andExpect(jsonPath("$.data.inactive").value(2));

        mockMvc.perform(get("/api/v1/patents/summary")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(1))
                .andExpect(jsonPath("$.data.inactive").value(1));
    }

    @Test
    void patentListSupportsLegalScreenFiltersAndExpandedFields() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Filtered Patent")
                .applicationNumber("APP-LIST-FILTER")
                .registrationNumber("REG-LIST-FILTER")
                .applicationDate(LocalDate.now().minusYears(1))
                .expiryDate(LocalDate.now().plusYears(1))
                .techField("반도체")
                .businessField("메모리")
                .summary("특허 요약")
                .keywords(java.util.List.of("공정", "소자"))
                .citationCount(7)
                .examinationClaimCount(9)
                .filingCountry("KR")
                .currentDepartment(department)
                .build());
        patentLegalStatusRepository.save(PatentLegalStatus.builder()
                .patent(patent)
                .status(PatentLegalStatusType.REGISTERED)
                .changedAt(LocalDate.now())
                .build());
        Review review = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());
        review.submit(BusinessOpinion.MAINTAIN, "유지", Instant.now());
        String legalToken = createActiveUserToken("legal-patent-list", "legal-patent-list@example.com", UserRole.LEGAL);

        mockMvc.perform(get("/api/v1/patents")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("departmentId", department.getId().toString())
                        .param("status", "REGISTERED")
                        .param("filingCountry", "KR")
                        .param("sort", "expiryDate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(patent.getId()))
                .andExpect(jsonPath("$.data.items[0].registrationNumber").value("REG-LIST-FILTER"))
                .andExpect(jsonPath("$.data.items[0].latestLegalStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.data.items[0].techField").value("반도체"))
                .andExpect(jsonPath("$.data.items[0].businessField").value("메모리"))
                .andExpect(jsonPath("$.data.items[0].summary").value("특허 요약"))
                .andExpect(jsonPath("$.data.items[0].keywords[0]").value("공정"))
                .andExpect(jsonPath("$.data.items[0].citationCount").value(7))
                .andExpect(jsonPath("$.data.items[0].examinationClaimCount").value(9))
                .andExpect(jsonPath("$.data.items[0].filingCountry").value("KR"))
                .andExpect(jsonPath("$.data.items[0].currentDepartmentId").value(department.getId()))
                .andExpect(jsonPath("$.data.items[0].currentDepartmentName").value(department.getName()))
                .andExpect(jsonPath("$.data.items[0].reviewStatus").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].opinion").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].checked").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].latestReportScore").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].latestReportStatus").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].isOverdue").doesNotExist());
    }

    @Test
    void patentApisReturnValidationDuplicateAndNotFoundErrors() throws Exception {
        String legalToken = createActiveUserToken("legal-errors", "legal-errors@example.com", UserRole.LEGAL);

        mockMvc.perform(post("/api/v1/patents")
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "applicationNumber": "APP-INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        Long patentId = createPatent(legalToken, "Existing Patent", "APP-EXISTING");

        mockMvc.perform(post("/api/v1/patents")
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duplicate Patent",
                                  "applicationNumber": "APP-EXISTING"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_APPLICATION_NUMBER"));

        mockMvc.perform(put("/api/v1/patents/{patentId}", patentId)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "applicationNumber": "APP-EXISTING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/patents/{patentId}", 999999L)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(put("/api/v1/patents/{patentId}", 999999L)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Missing Patent",
                                  "applicationNumber": "APP-MISSING"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/patents/{patentId}", 999999L)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));
    }

    @Test
    void deletingPatentRemovesAssignmentsButPreservesDepartment() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Assigned Patent")
                .applicationNumber("APP-ASSIGNED")
                .currentDepartment(department)
                .build());
        Report report = reportRepository.save(Report.builder()
                .patent(patent)
                .build());
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(delete("/api/v1/patents/{patentId}", patent.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(departmentRepository.existsById(department.getId())).isTrue();
        assertThat(reportRepository.existsById(report.getId())).isFalse();
        assertThat(patentRepository.existsById(patent.getId())).isFalse();
    }

    @Test
    void assignedPatentApisAllowBusinessDepartmentAccessAndReview() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Assigned Patent")
                .applicationNumber("APP-OPINION")
                .summary("사업부 검토 요약")
                .keywords(java.util.List.of("검토", "사업부"))
                .currentDepartment(department)
                .build());
        Review review = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());
        reportRepository.save(Report.builder()
                .patent(patent)
                .reportKey("reports/%d/report.html".formatted(patent.getId()))
                .totalScore(new BigDecimal("91.50"))
                .valueGrade("S")
                .status(ReportStatus.REPORT_COMPLETED)
                .evaluatedAt(Instant.now())
                .build());
        String businessToken = createActiveUserToken("business-opinion", "business-opinion@example.com", UserRole.BUSINESS);
        String legalToken = createActiveUserToken("legal-opinion", "legal-opinion@example.com", UserRole.LEGAL);

        mockMvc.perform(get("/api/v1/business-reviews"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/business-reviews")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/business-reviews/summary")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCycle.id").value(reviewCycle.getId()))
                .andExpect(jsonPath("$.data.kpi.submitted").value(0))
                .andExpect(jsonPath("$.data.kpi.notSubmitted").value(1));

        mockMvc.perform(get("/api/v1/business-reviews")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(review.getId()))
                .andExpect(jsonPath("$.data.items[0].patentId").value(patent.getId()))
                .andExpect(jsonPath("$.data.items[0].title").value("Assigned Patent"))
                .andExpect(jsonPath("$.data.items[0].applicationNumber").value("APP-OPINION"))
                .andExpect(jsonPath("$.data.items[0].summary").value("사업부 검토 요약"))
                .andExpect(jsonPath("$.data.items[0].keywords[0]").value("검토"))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.items[0].totalScore").value(91.5))
                .andExpect(jsonPath("$.data.items[0].valueGrade").value("S"))
                .andExpect(jsonPath("$.data.items[0].reviewRequestedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/business-reviews")
                        .header("Authorization", "Bearer " + businessToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(review.getId()))
                .andExpect(jsonPath("$.data.items[0].patentId").value(patent.getId()));

        mockMvc.perform(get("/api/v1/business-reviews")
                        .header("Authorization", "Bearer " + businessToken)
                        .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));

        mockMvc.perform(get("/api/v1/business-reviews/{patentId}", patent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patent.id").value(patent.getId()))
                .andExpect(jsonPath("$.data.patent.title").value("Assigned Patent"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.reviewRequestedAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/business-reviews/{patentId}/opinions", patent.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "opinion": "MAINTAIN",
                                  "comment": "핵심 특허로 판단됩니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinion").value("MAINTAIN"))
                .andExpect(jsonPath("$.data.comment").value("핵심 특허로 판단됩니다."))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());

        Review submitted = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(submitted.getStatus()).isEqualTo(ReviewStatus.SUBMITTED);
        assertThat(submitted.getSubmittedAt()).isNotNull();

        mockMvc.perform(get("/api/v1/business-reviews/summary")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kpi.submitted").value(1))
                .andExpect(jsonPath("$.data.kpi.notSubmitted").value(0));

        mockMvc.perform(get("/api/v1/business-reviews")
                        .header("Authorization", "Bearer " + businessToken)
                        .param("status", "SUBMITTED")
                        .param("opinion", "MAINTAIN")
                        .param("submittedFrom", LocalDate.now().toString())
                        .param("submittedTo", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(review.getId()))
                .andExpect(jsonPath("$.data.items[0].patentId").value(patent.getId()))
                .andExpect(jsonPath("$.data.items[0].opinion").value("MAINTAIN"));

        mockMvc.perform(get("/api/v1/business-reviews")
                        .header("Authorization", "Bearer " + businessToken)
                        .param("opinion", "HOLD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/v1/business-reviews/{patentId}/opinions", patent.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "opinion": "ABANDON"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("OPINION_ALREADY_SUBMITTED"));
    }

    @Test
    void assignedPatentApisRejectOtherDepartmentMissingIdAndInvalidOpinion() throws Exception {
        Department otherDepartment = departmentRepository.save(Department.builder()
                .name("제조")
                .build());
        Patent patent = patentRepository.save(Patent.builder()
                .title("Other Department Patent")
                .applicationNumber("APP-OTHER-DEPARTMENT")
                .currentDepartment(otherDepartment)
                .build());
        Review review = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(otherDepartment)
                .reviewCycle(reviewCycle)
                .build());
        String businessToken = createActiveUserToken("business-other", "business-other@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/business-reviews/{patentId}", patent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/business-reviews/{patentId}", 999999L)
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        patent.changeCurrentDepartment(department);
        Review ownReview = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());

        mockMvc.perform(post("/api/v1/business-reviews/{patentId}/opinions", patent.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "opinion": "보류"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void legalUserCanCreateReviewRequest() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Review Request Patent")
                .applicationNumber("APP-REVIEW-REQUEST")
                .currentDepartment(department)
                .build());
        String legalToken = createActiveUserToken("legal-review", "legal-review@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-review", "business-review@example.com", UserRole.BUSINESS);

        mockMvc.perform(post("/api/v1/patents/{patentId}/reviews", patent.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/patents/{patentId}/reviews", patent.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/patents/{patentId}/reviews", patent.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.patentId").value(patent.getId()))
                .andExpect(jsonPath("$.data.title").value("Review Request Patent"))
                .andExpect(jsonPath("$.data.applicationNumber").value("APP-REVIEW-REQUEST"))
                .andExpect(jsonPath("$.data.departmentId").value(department.getId()))
                .andExpect(jsonPath("$.data.departmentName").value("통신"))
                .andExpect(jsonPath("$.data.reviewCycleId").value(reviewCycle.getId()))
                .andExpect(jsonPath("$.data.reviewCycleYear").value(2026))
                .andExpect(jsonPath("$.data.reviewCycleQuarter").value(2))
                .andExpect(jsonPath("$.data.opinion").value(nullValue()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.submittedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.dueDate").value(reviewCycle.getDeadline().toString()));

        assertThat(reviewRepository.existsByReviewCycleIdAndPatentIdAndDepartmentId(
                reviewCycle.getId(),
                patent.getId(),
                department.getId()
        )).isTrue();
    }

    @Test
    void legalUserCanCreateReviewRequestWithReviewCycleDeadline() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Review Request Due Date Patent")
                .applicationNumber("APP-REVIEW-DUE-DATE")
                .currentDepartment(department)
                .build());
        String legalToken = createActiveUserToken("legal-review-due-date", "legal-review-due-date@example.com", UserRole.LEGAL);

        mockMvc.perform(post("/api/v1/patents/{patentId}/reviews", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dueDate").value(reviewCycle.getDeadline().toString()));

        Review review = reviewRepository.findByReviewCycleIdAndPatentIdAndDepartmentId(
                reviewCycle.getId(),
                patent.getId(),
                department.getId()
        ).orElseThrow();
        assertThat(review.getDueDate()).isEqualTo(reviewCycle.getDeadline());
    }


    @Test
    void reviewRequestCreationRejectsInvalidAndDuplicateRequests() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Review Error Patent")
                .applicationNumber("APP-REVIEW-ERROR")
                .currentDepartment(department)
                .build());
        Patent unassignedPatent = patentRepository.save(Patent.builder()
                .title("Unassigned Review Patent")
                .applicationNumber("APP-REVIEW-NO-DEPT")
                .build());
        String legalToken = createActiveUserToken("legal-review-error", "legal-review-error@example.com", UserRole.LEGAL);

        mockMvc.perform(post("/api/v1/patents/{patentId}/reviews", 999999L)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/patents/{patentId}/reviews", unassignedPatent.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PATENT_DEPARTMENT_NOT_ASSIGNED"));

        reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());

        mockMvc.perform(post("/api/v1/patents/{patentId}/reviews", patent.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_REVIEW_REQUEST"));
    }

    @Test
    void legalUserCanCreateBulkReviewRequestsWithPerPatentResults() throws Exception {
        Patent eligiblePatent = patentRepository.save(Patent.builder()
                .title("Bulk Eligible Patent")
                .applicationNumber("APP-BULK-ELIGIBLE")
                .currentDepartment(department)
                .build());
        Patent duplicatePatent = patentRepository.save(Patent.builder()
                .title("Bulk Duplicate Patent")
                .applicationNumber("APP-BULK-DUPLICATE")
                .currentDepartment(department)
                .build());
        Patent unassignedPatent = patentRepository.save(Patent.builder()
                .title("Bulk Unassigned Patent")
                .applicationNumber("APP-BULK-UNASSIGNED")
                .build());
        reviewRepository.save(Review.builder()
                .patent(duplicatePatent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());
        String legalToken = createActiveUserToken("legal-review-bulk", "legal-review-bulk@example.com", UserRole.LEGAL);

        mockMvc.perform(post("/api/v1/reviews/bulk")
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patentIds": [%d, %d, %d, 999999, %d]
                                }
                                """.formatted(
                                eligiblePatent.getId(),
                                duplicatePatent.getId(),
                                unassignedPatent.getId(),
                                eligiblePatent.getId()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reviewCycleId").value(reviewCycle.getId()))
                .andExpect(jsonPath("$.data.createdCount").value(1))
                .andExpect(jsonPath("$.data.skippedCount").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(4))
                .andExpect(jsonPath("$.data.items[0].patentId").value(eligiblePatent.getId()))
                .andExpect(jsonPath("$.data.items[0].status").value("CREATED"))
                .andExpect(jsonPath("$.data.items[1].reason").value("DUPLICATE_REVIEW_REQUEST"))
                .andExpect(jsonPath("$.data.items[2].reason").value("PATENT_DEPARTMENT_NOT_ASSIGNED"))
                .andExpect(jsonPath("$.data.items[3].reason").value("PATENT_NOT_FOUND"));

        assertThat(reviewRepository.existsByReviewCycleIdAndPatentIdAndDepartmentId(
                reviewCycle.getId(),
                eligiblePatent.getId(),
                department.getId()
        )).isTrue();
        Review review = reviewRepository.findByReviewCycleIdAndPatentIdAndDepartmentId(
                reviewCycle.getId(),
                eligiblePatent.getId(),
                department.getId()
        ).orElseThrow();
        assertThat(review.getDueDate()).isEqualTo(reviewCycle.getDeadline());
    }

    @Test
    void reviewRequestCreationAllowsNewRequestAfterPreviousSubmission() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Repeated Review Patent")
                .applicationNumber("APP-REVIEW-REPEATED")
                .currentDepartment(department)
                .build());
        ReviewCycle previousCycle = reviewCycleRepository.save(ReviewCycle.builder()
                .year(2026)
                .quarter(1)
                .startDate(LocalDate.now().minusMonths(3))
                .endDate(LocalDate.now().minusMonths(1))
                .build());
        Review submittedReview = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(previousCycle)
                .build());
        submittedReview.submit(BusinessOpinion.MAINTAIN, "기존 의견", Instant.now());
        String legalToken = createActiveUserToken("legal-review-repeat", "legal-review-repeat@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-review-repeat", "business-review-repeat@example.com", UserRole.BUSINESS);

        mockMvc.perform(post("/api/v1/patents/{patentId}/reviews", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertThat(reviewRepository.findAll()).hasSize(2);

        mockMvc.perform(get("/api/v1/business-reviews")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].patentId").value(patent.getId()))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"));
    }

    @Test
    void opinionSubmissionRejectsCompletedLatestReviewWithoutUpdatingOlderPendingReview() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Latest Review Patent")
                .applicationNumber("APP-REVIEW-LATEST")
                .currentDepartment(department)
                .build());
        ReviewCycle previousCycle = reviewCycleRepository.save(ReviewCycle.builder()
                .year(2026)
                .quarter(1)
                .startDate(LocalDate.now().minusMonths(3))
                .endDate(LocalDate.now().minusMonths(1))
                .build());
        Review previousReview = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(previousCycle)
                .build());
        Review latestReview = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());
        latestReview.submit(BusinessOpinion.MAINTAIN, "최신 의견", Instant.now());
        String businessToken = createActiveUserToken("business-latest-review", "business-latest-review@example.com", UserRole.BUSINESS);

        mockMvc.perform(post("/api/v1/business-reviews/{patentId}/opinions", patent.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "opinion": "ABANDON",
                                  "comment": "과거 요청을 변경하면 안 됩니다."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("OPINION_ALREADY_SUBMITTED"));

        assertThat(previousReview.getStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThat(previousReview.getOpinion()).isNull();
    }

    @Test
    void opinionSubmissionAllowsOverdueReviewRequestInCurrentCycle() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Overdue Review Patent")
                .applicationNumber("APP-REVIEW-OVERDUE")
                .currentDepartment(department)
                .build());
        reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .status(ReviewStatus.OVERDUE)
                .dueDate(LocalDate.now().minusDays(1))
                .build());
        String businessToken = createActiveUserToken("business-expired-review", "business-expired-review@example.com", UserRole.BUSINESS);

        mockMvc.perform(post("/api/v1/business-reviews/{patentId}/opinions", patent.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "opinion": "MAINTAIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.opinion").value("MAINTAIN"));
    }

    @Test
    void businessReviewHistoryReturnsPastSubmittedReviewsByYearAndQuarter() throws Exception {
        Department otherDepartment = departmentRepository.save(Department.builder()
                .name("제조")
                .build());
        ReviewCycle cycle2026Q1 = reviewCycleRepository.save(ReviewCycle.builder()
                .year(2026)
                .quarter(1)
                .startDate(LocalDate.now().minusMonths(5))
                .endDate(LocalDate.now().minusMonths(3))
                .build());
        ReviewCycle cycle2025Q4 = reviewCycleRepository.save(ReviewCycle.builder()
                .year(2025)
                .quarter(4)
                .startDate(LocalDate.now().minusMonths(8))
                .endDate(LocalDate.now().minusMonths(6))
                .build());
        Patent latestPastPatent = patentRepository.save(Patent.builder()
                .title("Latest Past Review Patent")
                .applicationNumber("HIST-2026-Q1")
                .currentDepartment(department)
                .build());
        Patent olderPastPatent = patentRepository.save(Patent.builder()
                .title("Older Past Review Patent")
                .applicationNumber("HIST-2025-Q4")
                .currentDepartment(department)
                .build());
        Patent currentPatent = patentRepository.save(Patent.builder()
                .title("Current Submitted Review Patent")
                .applicationNumber("HIST-CURRENT")
                .currentDepartment(department)
                .build());
        Patent pendingPatent = patentRepository.save(Patent.builder()
                .title("Past Pending Review Patent")
                .applicationNumber("HIST-PENDING")
                .currentDepartment(department)
                .build());
        Patent otherPatent = patentRepository.save(Patent.builder()
                .title("Other Department Past Review Patent")
                .applicationNumber("HIST-OTHER")
                .currentDepartment(otherDepartment)
                .build());
        reviewRepository.save(Review.builder()
                .patent(latestPastPatent)
                .department(department)
                .reviewCycle(cycle2026Q1)
                .opinion(BusinessOpinion.MAINTAIN)
                .comment("2026 Q1 제출")
                .status(ReviewStatus.SUBMITTED)
                .submittedAt(Instant.now().minusSeconds(20))
                .build());
        reviewRepository.save(Review.builder()
                .patent(olderPastPatent)
                .department(department)
                .reviewCycle(cycle2025Q4)
                .opinion(BusinessOpinion.ABANDON)
                .comment("2025 Q4 제출")
                .status(ReviewStatus.SUBMITTED)
                .submittedAt(Instant.now().minusSeconds(40))
                .build());
        reviewRepository.save(Review.builder()
                .patent(currentPatent)
                .department(department)
                .reviewCycle(reviewCycle)
                .opinion(BusinessOpinion.MAINTAIN)
                .status(ReviewStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build());
        reviewRepository.save(Review.builder()
                .patent(pendingPatent)
                .department(department)
                .reviewCycle(cycle2025Q4)
                .status(ReviewStatus.PENDING)
                .build());
        reviewRepository.save(Review.builder()
                .patent(otherPatent)
                .department(otherDepartment)
                .reviewCycle(cycle2025Q4)
                .opinion(BusinessOpinion.MAINTAIN)
                .status(ReviewStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build());
        reportRepository.save(Report.builder()
                .patent(latestPastPatent)
                .totalScore(new BigDecimal("90.00"))
                .valueGrade("S")
                .status(ReportStatus.REPORT_COMPLETED)
                .build());
        String businessToken = createActiveUserToken("business-review-history", "business-review-history@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/business-reviews/history")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].patentId").value(latestPastPatent.getId()))
                .andExpect(jsonPath("$.data.items[0].reviewCycle.year").value(2026))
                .andExpect(jsonPath("$.data.items[0].reviewCycle.quarter").value(1))
                .andExpect(jsonPath("$.data.items[0].totalScore").value(90.0))
                .andExpect(jsonPath("$.data.items[0].valueGrade").value("S"))
                .andExpect(jsonPath("$.data.items[1].patentId").value(olderPastPatent.getId()));

        mockMvc.perform(get("/api/v1/business-reviews/history")
                        .header("Authorization", "Bearer " + businessToken)
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].patentId").value(olderPastPatent.getId()))
                .andExpect(jsonPath("$.data.items[0].reviewCycle.year").value(2025));

        mockMvc.perform(get("/api/v1/business-reviews/history")
                        .header("Authorization", "Bearer " + businessToken)
                        .param("year", "2025")
                        .param("quarter", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].reviewCycle.quarter").value(4));

        mockMvc.perform(get("/api/v1/business-reviews/history")
                        .header("Authorization", "Bearer " + businessToken)
                        .param("quarter", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void businessUserCanReadOnlyAssignedPatentHistory() throws Exception {
        Department otherDepartment = departmentRepository.save(Department.builder()
                .name("제조")
                .build());
        Patent assignedPatent = patentRepository.save(Patent.builder()
                .title("Assigned History Patent")
                .applicationNumber("APP-HISTORY-OWN")
                .currentDepartment(department)
                .build());
        Patent otherPatent = patentRepository.save(Patent.builder()
                .title("Other History Patent")
                .applicationNumber("APP-HISTORY-OTHER")
                .currentDepartment(otherDepartment)
                .build());
        String businessToken = createActiveUserToken("business-history", "business-history@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/patents/{patentId}/annuities", assignedPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/patents/{patentId}/legal-status", assignedPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/patents/{patentId}/annuities", otherPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/patents/{patentId}/legal-status", otherPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void businessUserCanReadOnlyAssignedPatentReports() throws Exception {
        Department otherDepartment = departmentRepository.save(Department.builder()
                .name("제조")
                .build());
        Patent assignedPatent = patentRepository.save(Patent.builder()
                .title("Assigned Report Patent")
                .applicationNumber("APP-REPORT-OWN")
                .currentDepartment(department)
                .build());
        Patent otherPatent = patentRepository.save(Patent.builder()
                .title("Other Report Patent")
                .applicationNumber("APP-REPORT-OTHER")
                .currentDepartment(otherDepartment)
                .build());
        Report assignedReport = reportRepository.save(Report.builder()
                .patent(assignedPatent)
                .reportKey("reports/assigned/report.html")
                .status(ReportStatus.REPORT_COMPLETED)
                .evaluatedAt(Instant.parse("2026-06-07T08:55:00Z"))
                .build());
        Report otherReport = reportRepository.save(Report.builder()
                .patent(otherPatent)
                .build());
        String businessToken = createActiveUserToken("business-report", "business-report@example.com", UserRole.BUSINESS);
        when(reportStorageService.generatePresignedUrl("reports/assigned/report.html"))
                .thenReturn("https://minio.example.com/reports/assigned/report.html?signature=abc");

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports", assignedPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(assignedReport.getId()));

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}", assignedPatent.getId(), assignedReport.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assignedReport.getId()))
                .andExpect(jsonPath("$.data.url").value("https://minio.example.com/reports/assigned/report.html?signature=abc"));

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}/status", assignedPatent.getId(), assignedReport.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assignedReport.getId()));

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports", otherPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}", otherPatent.getId(), otherReport.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}/status", otherPatent.getId(), otherReport.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void legalUserCanReadAndFilterReviews() throws Exception {
        Department otherDepartment = departmentRepository.save(Department.builder()
                .name("제조")
                .build());
        Patent firstPatent = patentRepository.save(Patent.builder()
                .title("First Review Patent")
                .applicationNumber("APP-REVIEW-FIRST")
                .techField("Battery")
                .businessField("Energy")
                .currentDepartment(department)
                .build());
        Patent secondPatent = patentRepository.save(Patent.builder()
                .title("Second Review Patent")
                .applicationNumber("APP-REVIEW-SECOND")
                .techField("Semiconductor")
                .businessField("Memory")
                .currentDepartment(otherDepartment)
                .build());
        Review firstReview = reviewRepository.save(Review.builder()
                .patent(firstPatent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());
        Review secondReview = reviewRepository.save(Review.builder()
                .patent(secondPatent)
                .department(otherDepartment)
                .reviewCycle(reviewCycle)
                .build());
        secondReview.submit(BusinessOpinion.MAINTAIN, "유지 의견입니다.", Instant.now());

        String legalToken = createActiveUserToken("legal-review-read", "legal-review-read@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-review-read", "business-review-read@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/api/v1/review-targets")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/review-targets")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(firstReview.getId()))
                .andExpect(jsonPath("$.data.items[0].techField").value("Battery"))
                .andExpect(jsonPath("$.data.items[0].businessField").value("Energy"))
                .andExpect(jsonPath("$.data.items[1].id").value(secondReview.getId()));

        mockMvc.perform(get("/api/v1/review-targets")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(firstReview.getId()))
                .andExpect(jsonPath("$.data.items[1].id").value(secondReview.getId()));

        mockMvc.perform(get("/api/v1/review-targets")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("status", "SUBMITTED")
                        .param("checked", "false")
                        .param("departmentId", otherDepartment.getId().toString())
                        .param("patentId", secondPatent.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(secondReview.getId()))
                .andExpect(jsonPath("$.data.items[0].opinion").value("MAINTAIN"))
                .andExpect(jsonPath("$.data.items[0].comment").value("유지 의견입니다."))
                .andExpect(jsonPath("$.data.items[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.items[0].techField").value("Semiconductor"))
                .andExpect(jsonPath("$.data.items[0].businessField").value("Memory"))
                .andExpect(jsonPath("$.data.items[0].checked").value(false));

        mockMvc.perform(get("/api/v1/review-targets/{reviewId}", secondReview.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(secondReview.getId()))
                .andExpect(jsonPath("$.data.patentId").value(secondPatent.getId()))
                .andExpect(jsonPath("$.data.departmentId").value(otherDepartment.getId()))
                .andExpect(jsonPath("$.data.opinion").value("MAINTAIN"))
                .andExpect(jsonPath("$.data.techField").value("Semiconductor"))
                .andExpect(jsonPath("$.data.businessField").value("Memory"))
                .andExpect(jsonPath("$.data.checked").value(false))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}/confirm", secondReview.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}/confirm", secondReview.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(secondReview.getId()))
                .andExpect(jsonPath("$.data.checked").value(true));

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}/confirm", secondReview.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checked").value(true));

        mockMvc.perform(get("/api/v1/review-targets")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("status", "SUBMITTED")
                        .param("checked", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));

        mockMvc.perform(get("/api/v1/review-targets")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("status", "SUBMITTED")
                        .param("checked", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].checked").value(true));

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}/confirm", firstReview.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_REVIEW_STATUS"));
    }

    @Test
    void legalReviewQueriesRejectInvalidStatusAndMissingId() throws Exception {
        String legalToken = createActiveUserToken("legal-review-query-error", "legal-review-query-error@example.com", UserRole.LEGAL);

        mockMvc.perform(get("/api/v1/review-targets")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("status", "대기"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/review-targets")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("sort", "inventor,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/review-targets/{reviewId}", 999999L)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_FOUND"));

        mockMvc.perform(patch("/api/v1/reviews/{reviewId}/confirm", 999999L)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_FOUND"));
    }

    @Test
    void adminCanReadOperationalData() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "admin-password");
        Patent patent = patentRepository.save(Patent.builder()
                .title("Admin Read Patent")
                .applicationNumber("APP-ADMIN-READ")
                .currentDepartment(department)
                .build());
        Report report = reportRepository.save(Report.builder()
                .patent(patent)
                .reportKey("reports/admin/report.html")
                .status(ReportStatus.REPORT_COMPLETED)
                .evaluatedAt(Instant.parse("2026-06-07T08:55:00Z"))
                .build());
        Review review = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());
        when(reportStorageService.generatePresignedUrl("reports/admin/report.html"))
                .thenReturn("https://minio.example.com/reports/admin/report.html?signature=abc");

        mockMvc.perform(get("/api/v1/patents/{patentId}/legal-status", patent.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/patents/{patentId}/annuities", patent.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports", patent.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}", patent.getId(), report.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://minio.example.com/reports/admin/report.html?signature=abc"));

        mockMvc.perform(get("/api/v1/patents/{patentId}/reports/{reportId}/status", patent.getId(), report.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/review-cycles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/review-cycles/{reviewCycleId}", reviewCycle.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/review-targets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/review-targets/{reviewId}", review.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotPerformLegalOperationalWrites() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "admin-password");
        Patent patent = patentRepository.save(Patent.builder()
                .title("Admin Write Patent")
                .applicationNumber("APP-ADMIN-WRITE")
                .currentDepartment(department)
                .build());

        mockMvc.perform(post("/api/v1/patents/{patentId}/legal-status", patent.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PUBLISHED",
                                  "changedAt": "2026-06-02"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/patents/{patentId}/annuities", patent.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentYears": 1,
                                  "amount": 100000
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/patents/{patentId}/reports", patent.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/patents/{patentId}/reviews", patent.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/reviews/bulk")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patentIds": [%d]
                                }
                                """.formatted(patent.getId())))
                .andExpect(status().isForbidden());

    }

    @Test
    void adminCanManageReviewCyclesAndUsersCanReadThem() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "admin-password");
        String legalToken = createActiveUserToken("legal-review-cycle", "legal-review-cycle@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-review-cycle", "business-review-cycle@example.com", UserRole.BUSINESS);
        LocalDate startDate = LocalDate.now().plusMonths(1);
        LocalDate endDate = LocalDate.now().plusMonths(2);

        mockMvc.perform(get("/api/v1/review-cycles")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/review-cycles/current")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reviewCycle.getId()));

        mockMvc.perform(post("/api/v1/review-cycles")
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2027,
                                  "quarter": 1,
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(startDate, endDate)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        MvcResult createResult = mockMvc.perform(post("/api/v1/review-cycles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2027,
                                  "quarter": 1,
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(startDate, endDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.year").value(2027))
                .andExpect(jsonPath("$.data.quarter").value(1))
                .andExpect(jsonPath("$.data.startDate").value(startDate.toString()))
                .andExpect(jsonPath("$.data.endDate").value(endDate.toString()))
                .andReturn();

        Long reviewCycleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/v1/review-cycles")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(reviewCycleId));

        mockMvc.perform(get("/api/v1/review-cycles/{reviewCycleId}", reviewCycleId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reviewCycleId));

        LocalDate updatedStartDate = LocalDate.now().plusMonths(3);
        LocalDate updatedEndDate = LocalDate.now().plusMonths(4);
        mockMvc.perform(put("/api/v1/review-cycles/{reviewCycleId}", reviewCycleId)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2027,
                                  "quarter": 2,
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(updatedStartDate, updatedEndDate)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/v1/review-cycles/{reviewCycleId}", reviewCycleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2027,
                                  "quarter": 2,
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(updatedStartDate, updatedEndDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.year").value(2027))
                .andExpect(jsonPath("$.data.quarter").value(2))
                .andExpect(jsonPath("$.data.startDate").value(updatedStartDate.toString()))
                .andExpect(jsonPath("$.data.endDate").value(updatedEndDate.toString()));

        mockMvc.perform(delete("/api/v1/review-cycles/{reviewCycleId}", reviewCycleId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(delete("/api/v1/review-cycles/{reviewCycleId}", reviewCycleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/review-cycles/{reviewCycleId}", reviewCycleId)
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REVIEW_CYCLE_NOT_FOUND"));
    }

    @Test
    void registrationRejectsUnsupportedRoleAsInvalidRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "unsupported-role",
                                  "password": "password",
                                  "name": "Unsupported Role",
                                  "email": "unsupported-role@example.com",
                                  "role": "MANAGER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_ROLE"));
    }

    @Test
    void registrationReturnsDuplicateAndValidationErrors() throws Exception {
        registerBusinessUser();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "business-new",
                                  "password": "password",
                                  "name": "Duplicate Login",
                                  "email": "different@example.com",
                                  "role": "BUSINESS"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_LOGIN_ID"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "different",
                                  "password": "password",
                                  "name": "Duplicate Email",
                                  "email": "business-new@example.com",
                                  "role": "BUSINESS"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "admin-signup",
                                  "password": "password",
                                  "name": "Admin Signup",
                                  "email": "admin-signup@example.com",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ROLE"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "invalid-email",
                                  "password": "password",
                                  "name": "Invalid Email",
                                  "email": "not-an-email",
                                  "role": "BUSINESS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void registrationRejectsFieldValuesLongerThanPersistenceLimits() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "%s",
                                  "password": "password",
                                  "name": "Too Long Login",
                                  "email": "long-login@example.com",
                                  "role": "BUSINESS"
                                }
                                """.formatted("a".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "long-name",
                                  "password": "password",
                                  "name": "%s",
                                  "email": "long-name@example.com",
                                  "role": "BUSINESS"
                                }
                                """.formatted("n".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "long-email",
                                  "password": "password",
                                  "name": "Long Email",
                                  "email": "%s@example.com",
                                  "role": "BUSINESS"
                                }
                                """.formatted("e".repeat(190))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void departmentWriteRejectsNameLongerThanPersistenceLimit() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "admin-password");
        String longName = "d".repeat(101);

        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s"
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(put("/api/v1/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s"
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void databaseConstraintRejectsExactDuplicateDepartmentName() {
        departmentRepository.saveAndFlush(Department.builder().name("Legal").build());

        assertThatThrownBy(() -> departmentRepository.saveAndFlush(
                Department.builder().name("Legal").build()
        )).isInstanceOf(RuntimeException.class);
    }

    @Test
    void databaseConstraintRejectsDuplicateUserLoginId() {
        assertThatThrownBy(() -> userRepository.saveAndFlush(User.createActive(
                "admin",
                "Other Admin",
                "other-admin@example.com",
                passwordEncoder.encode("password"),
                UserRole.ADMIN,
                null
        ))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void databaseConstraintRejectsDuplicateUserEmail() {
        assertThatThrownBy(() -> userRepository.saveAndFlush(User.createActive(
                "other-admin",
                "Other Admin",
                "admin@example.com",
                passwordEncoder.encode("password"),
                UserRole.ADMIN,
                null
        ))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void databaseConstraintRejectsDuplicatePatentApplicationNumber() {
        patentRepository.saveAndFlush(Patent.builder()
                .title("Patent")
                .applicationNumber("APP-UNIQUE")
                .build());

        assertThatThrownBy(() -> patentRepository.saveAndFlush(Patent.builder()
                .title("Duplicate Patent")
                .applicationNumber("APP-UNIQUE")
                .build())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void persistenceAuditingPopulatesDepartmentTimestamps() {
        Department saved = departmentRepository.saveAndFlush(Department.builder()
                .name("Audited Department")
                .build());

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void malformedJsonReturnsInvalidRequestErrorResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "loginId": "malformed",
                                  "password": "password"
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void nonNumericApprovalUserIdReturnsInvalidRequestErrorResponse() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(patch("/api/v1/admin/users/not-a-number/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": %d
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    private Long registerBusinessUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "business-new",
                                  "password": "password",
                                  "name": "Business User",
                                  "email": "business-new@example.com",
                                  "role": "BUSINESS"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.departmentId").value(nullValue()))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .longValue();
    }

    private Long createPatent(String token, String title, String applicationNumber) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/patents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "applicationNumber": "%s"
                                }
                                """.formatted(title, applicationNumber)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .longValue();
    }

    private String loginAndGetAccessToken(String loginId, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "%s",
                                  "password": "%s"
                                }
                                """.formatted(loginId, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("accessToken").textValue();
    }

    private String createActiveUserToken(String loginId, String email, UserRole role) {
        User user = userRepository.save(User.createActive(
                loginId,
                loginId,
                email,
                passwordEncoder.encode("password"),
                role,
                department
        ));
        return jwtProvider.createAccessToken(user.getId(), role);
    }
}
