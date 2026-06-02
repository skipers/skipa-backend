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
import com.skipers.skipa.domain.review.domain.ReviewCycleType;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
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
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewCycleRepository reviewCycleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

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
                .name("2026년 2분기 정기 재평가")
                .type(ReviewCycleType.QUARTERLY)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
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

        mockMvc.perform(post("/auth/login")
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

        mockMvc.perform(get("/departments")
                        .header("Authorization", "Bearer " + pendingToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PENDING_USER"));
    }

    @Test
    void approvedBusinessUserBecomesActiveAndCanLogInButCannotReadDepartments() throws Exception {
        Long userId = registerBusinessUser();
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(patch("/admin/users/{userId}/approve", userId)
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

        mockMvc.perform(get("/departments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void approvedLegalUserBecomesActiveWithoutDepartment() throws Exception {
        MvcResult registrationResult = mockMvc.perform(post("/auth/register")
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

        mockMvc.perform(patch("/admin/users/{userId}/approve", userId)
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
    void approvalRequiresAdminAndReturnsRequestAndDomainErrors() throws Exception {
        Long userId = registerBusinessUser();
        String legalToken = createActiveUserToken("legal-approver", "legal-approver@example.com", UserRole.LEGAL);
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(patch("/admin/users/{userId}/approve", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": %d
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/admin/users/{userId}/approve", userId)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": %d
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/admin/users/{userId}/approve", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(patch("/admin/users/{userId}/approve", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": 999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEPARTMENT_NOT_FOUND"));

        mockMvc.perform(patch("/admin/users/{userId}/approve", 999999L)
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

        mockMvc.perform(get("/departments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/departments")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/departments")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/departments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void departmentWriteApisAllowOnlyAdmin() throws Exception {
        String legalToken = createActiveUserToken("legal-active", "legal-active@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-active", "business-active@example.com", UserRole.BUSINESS);
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unauthenticated Create"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/departments/{departmentId}", department.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unauthenticated Update"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/departments/{departmentId}", department.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Business Create"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Legal Update"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        MvcResult createResult = mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + adminToken)
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

        mockMvc.perform(put("/departments/{departmentId}", createdDepartmentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Department"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Department"));

        mockMvc.perform(delete("/departments/{departmentId}", createdDepartmentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void assignedDepartmentCannotBeDeleted() throws Exception {
        createActiveUserToken("assigned-business", "assigned-business@example.com", UserRole.BUSINESS);
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(delete("/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DEPARTMENT_IN_USE"));

        assertThat(departmentRepository.existsById(department.getId())).isTrue();
    }

    @Test
    void departmentApisReturnDuplicateAndNotFoundErrors() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "통신"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_DEPARTMENT_NAME"));

        mockMvc.perform(get("/departments/{departmentId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEPARTMENT_NOT_FOUND"));

        mockMvc.perform(put("/departments/{departmentId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unknown"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEPARTMENT_NOT_FOUND"));

        mockMvc.perform(delete("/departments/{departmentId}", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEPARTMENT_NOT_FOUND"));
    }

    @Test
    void departmentSearchUsesKeywordAndPagingAgainstDatabase() throws Exception {
        departmentRepository.save(Department.builder().name("Legal Affairs").build());
        departmentRepository.save(Department.builder().name("Legal Operations").build());
        String legalToken = createActiveUserToken("legal-reader", "legal-reader@example.com", UserRole.LEGAL);

        mockMvc.perform(get("/departments")
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

        mockMvc.perform(get("/patents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/patents")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/patents/{patentId}", 1L)
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/patents")
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Business Patent",
                                  "applicationNumber": "BUSINESS-1"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/patents/{patentId}", 1L)
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Business Update",
                                  "applicationNumber": "BUSINESS-2"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/patents/{patentId}", 1L)
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        MvcResult createResult = mockMvc.perform(post("/patents")
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Chip Patent  ",
                                  "applicationNumber": " APP-1 ",
                                  "relatedProducts": [" Product "],
                                  "initialDepartment": " Initial Legal ",
                                  "keywords": [" Keyword "]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("  Chip Patent  "))
                .andExpect(jsonPath("$.data.applicationNumber").value(" APP-1 "))
                .andExpect(jsonPath("$.data.relatedProducts[0]").value(" Product "))
                .andExpect(jsonPath("$.data.initialDepartment").value(" Initial Legal "))
                .andExpect(jsonPath("$.data.keywords[0]").value(" Keyword "))
                .andReturn();

        Long patentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .longValue();

        mockMvc.perform(get("/patents/{patentId}", patentId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relatedProducts[0]").value(" Product "))
                .andExpect(jsonPath("$.data.keywords[0]").value(" Keyword "));

        mockMvc.perform(get("/patents")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", " chip ")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("  Chip Patent  "));

        mockMvc.perform(put("/patents/{patentId}", patentId)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Patent",
                                  "applicationNumber": "APP-UPDATED",
                                  "relatedProducts": ["Updated Product"],
                                  "initialDepartment": "Updated Legal",
                                  "keywords": ["Updated Keyword"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Patent"))
                .andExpect(jsonPath("$.data.relatedProducts[0]").value("Updated Product"))
                .andExpect(jsonPath("$.data.initialDepartment").value("Updated Legal"));

        mockMvc.perform(delete("/patents/{patentId}", patentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(patentRepository.existsById(patentId)).isFalse();
    }

    @Test
    void patentApisReturnValidationDuplicateAndNotFoundErrors() throws Exception {
        String legalToken = createActiveUserToken("legal-errors", "legal-errors@example.com", UserRole.LEGAL);

        mockMvc.perform(post("/patents")
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

        mockMvc.perform(post("/patents")
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

        mockMvc.perform(put("/patents/{patentId}", patentId)
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

        mockMvc.perform(get("/patents/{patentId}", 999999L)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(put("/patents/{patentId}", 999999L)
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

        mockMvc.perform(delete("/patents/{patentId}", 999999L)
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
        String adminToken = loginAndGetAccessToken("admin", "admin-password");

        mockMvc.perform(delete("/patents/{patentId}", patent.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(departmentRepository.existsById(department.getId())).isTrue();
        assertThat(patentRepository.existsById(patent.getId())).isFalse();
    }

    @Test
    void assignedPatentApisAllowBusinessDepartmentAccessAndReview() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Assigned Patent")
                .applicationNumber("APP-OPINION")
                .currentDepartment(department)
                .build());
        Review review = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());
        String businessToken = createActiveUserToken("business-opinion", "business-opinion@example.com", UserRole.BUSINESS);
        String legalToken = createActiveUserToken("legal-opinion", "legal-opinion@example.com", UserRole.LEGAL);

        mockMvc.perform(get("/assigned-patents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/assigned-patents")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/assigned-patents")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(patent.getId()))
                .andExpect(jsonPath("$.data.items[0].title").value("Assigned Patent"))
                .andExpect(jsonPath("$.data.items[0].applicationNumber").value("APP-OPINION"))
                .andExpect(jsonPath("$.data.items[0].status").value("미제출"))
                .andExpect(jsonPath("$.data.items[0].reviewRequestedAt").isNotEmpty());

        mockMvc.perform(get("/assigned-patents/{patentId}", patent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patent.id").value(patent.getId()))
                .andExpect(jsonPath("$.data.patent.title").value("Assigned Patent"))
                .andExpect(jsonPath("$.data.status").value("미제출"))
                .andExpect(jsonPath("$.data.reviewRequestedAt").isNotEmpty());

        mockMvc.perform(post("/assigned-patents/{patentId}/opinions", patent.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "opinion": "유지",
                                  "comment": "핵심 특허로 판단됩니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opinion").value("유지"))
                .andExpect(jsonPath("$.data.comment").value("핵심 특허로 판단됩니다."))
                .andExpect(jsonPath("$.data.status").value("제출완료"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());

        Review submitted = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(submitted.getStatus()).isEqualTo(ReviewStatus.제출완료);
        assertThat(submitted.getSubmittedAt()).isNotNull();

        mockMvc.perform(post("/assigned-patents/{patentId}/opinions", patent.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "opinion": "포기"
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

        mockMvc.perform(get("/assigned-patents/{patentId}", patent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/assigned-patents/{patentId}", 999999L)
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        patent.changeCurrentDepartment(department);
        Review ownReview = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());

        mockMvc.perform(post("/assigned-patents/{patentId}/opinions", patent.getId())
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

        mockMvc.perform(post("/patents/{patentId}/reviews", patent.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/patents/{patentId}/reviews", patent.getId())
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/patents/{patentId}/reviews", patent.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.patentId").value(patent.getId()))
                .andExpect(jsonPath("$.data.title").value("Review Request Patent"))
                .andExpect(jsonPath("$.data.applicationNumber").value("APP-REVIEW-REQUEST"))
                .andExpect(jsonPath("$.data.departmentId").value(department.getId()))
                .andExpect(jsonPath("$.data.departmentName").value("통신"))
                .andExpect(jsonPath("$.data.reviewCycleId").value(reviewCycle.getId()))
                .andExpect(jsonPath("$.data.reviewCycleName").value("2026년 2분기 정기 재평가"))
                .andExpect(jsonPath("$.data.opinion").value(nullValue()))
                .andExpect(jsonPath("$.data.status").value("미제출"))
                .andExpect(jsonPath("$.data.submittedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.dueDate").value(reviewCycle.getEndDate().toString()));

        assertThat(reviewRepository.existsByReviewCycleIdAndPatentIdAndDepartmentId(
                reviewCycle.getId(),
                patent.getId(),
                department.getId()
        )).isTrue();
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

        mockMvc.perform(post("/patents/{patentId}/reviews", 999999L)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATENT_NOT_FOUND"));

        mockMvc.perform(post("/patents/{patentId}/reviews", unassignedPatent.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PATENT_DEPARTMENT_NOT_ASSIGNED"));

        reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());

        mockMvc.perform(post("/patents/{patentId}/reviews", patent.getId())
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_REVIEW_REQUEST"));
    }

    @Test
    void reviewRequestCreationAllowsNewRequestAfterPreviousSubmission() throws Exception {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Repeated Review Patent")
                .applicationNumber("APP-REVIEW-REPEATED")
                .currentDepartment(department)
                .build());
        ReviewCycle previousCycle = reviewCycleRepository.save(ReviewCycle.builder()
                .name("2026년 1분기 정기 재평가")
                .type(ReviewCycleType.QUARTERLY)
                .startDate(LocalDate.now().minusMonths(3))
                .endDate(LocalDate.now().minusMonths(1))
                .build());
        Review submittedReview = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(previousCycle)
                .build());
        submittedReview.submit(BusinessOpinion.유지, "기존 의견", Instant.now());
        String legalToken = createActiveUserToken("legal-review-repeat", "legal-review-repeat@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-review-repeat", "business-review-repeat@example.com", UserRole.BUSINESS);

        mockMvc.perform(post("/patents/{patentId}/reviews", patent.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("미제출"));

        assertThat(reviewRepository.findAll()).hasSize(2);

        mockMvc.perform(get("/assigned-patents")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(patent.getId()))
                .andExpect(jsonPath("$.data.items[0].status").value("미제출"));
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

        mockMvc.perform(get("/patents/{patentId}/annuities", assignedPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/patents/{patentId}/legal-status", assignedPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/patents/{patentId}/annuities", otherPatent.getId())
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/patents/{patentId}/legal-status", otherPatent.getId())
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
                .build());
        Patent secondPatent = patentRepository.save(Patent.builder()
                .title("Second Review Patent")
                .applicationNumber("APP-REVIEW-SECOND")
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
        secondReview.submit(BusinessOpinion.유지, "유지 의견입니다.", Instant.now());

        String legalToken = createActiveUserToken("legal-review-read", "legal-review-read@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-review-read", "business-review-read@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(secondReview.getId()))
                .andExpect(jsonPath("$.data.items[1].id").value(firstReview.getId()));

        mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("status", "제출완료")
                        .param("departmentId", otherDepartment.getId().toString())
                        .param("patentId", secondPatent.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(secondReview.getId()))
                .andExpect(jsonPath("$.data.items[0].opinion").value("유지"))
                .andExpect(jsonPath("$.data.items[0].comment").value("유지 의견입니다."))
                .andExpect(jsonPath("$.data.items[0].status").value("제출완료"));

        mockMvc.perform(get("/reviews/{reviewId}", secondReview.getId())
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(secondReview.getId()))
                .andExpect(jsonPath("$.data.patentId").value(secondPatent.getId()))
                .andExpect(jsonPath("$.data.departmentId").value(otherDepartment.getId()))
                .andExpect(jsonPath("$.data.opinion").value("유지"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());
    }

    @Test
    void legalReviewQueriesRejectInvalidStatusAndMissingId() throws Exception {
        String legalToken = createActiveUserToken("legal-review-query-error", "legal-review-query-error@example.com", UserRole.LEGAL);

        mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("status", "대기"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/reviews/{reviewId}", 999999L)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_FOUND"));
    }

    @Test
    void legalUserCanManageReviewCycles() throws Exception {
        String legalToken = createActiveUserToken("legal-review-cycle", "legal-review-cycle@example.com", UserRole.LEGAL);
        String businessToken = createActiveUserToken("business-review-cycle", "business-review-cycle@example.com", UserRole.BUSINESS);
        LocalDate startDate = LocalDate.now().plusMonths(1);
        LocalDate endDate = LocalDate.now().plusMonths(2);

        mockMvc.perform(get("/review-cycles")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        MvcResult createResult = mockMvc.perform(post("/review-cycles")
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "2026년 하반기 수시 재평가",
                                  "type": "AD_HOC",
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(startDate, endDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("2026년 하반기 수시 재평가"))
                .andExpect(jsonPath("$.data.type").value("AD_HOC"))
                .andExpect(jsonPath("$.data.startDate").value(startDate.toString()))
                .andExpect(jsonPath("$.data.endDate").value(endDate.toString()))
                .andReturn();

        Long reviewCycleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(get("/review-cycles")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(reviewCycleId));

        mockMvc.perform(get("/review-cycles/{reviewCycleId}", reviewCycleId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reviewCycleId));

        LocalDate updatedStartDate = LocalDate.now().plusMonths(3);
        LocalDate updatedEndDate = LocalDate.now().plusMonths(4);
        mockMvc.perform(put("/review-cycles/{reviewCycleId}", reviewCycleId)
                        .header("Authorization", "Bearer " + legalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "2026년 하반기 정기 재평가",
                                  "type": "QUARTERLY",
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(updatedStartDate, updatedEndDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("2026년 하반기 정기 재평가"))
                .andExpect(jsonPath("$.data.type").value("QUARTERLY"))
                .andExpect(jsonPath("$.data.startDate").value(updatedStartDate.toString()))
                .andExpect(jsonPath("$.data.endDate").value(updatedEndDate.toString()));

        mockMvc.perform(delete("/review-cycles/{reviewCycleId}", reviewCycleId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/review-cycles/{reviewCycleId}", reviewCycleId)
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REVIEW_CYCLE_NOT_FOUND"));
    }

    @Test
    void registrationRejectsUnsupportedRoleAsInvalidRole() throws Exception {
        mockMvc.perform(post("/auth/register")
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

        mockMvc.perform(post("/auth/register")
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

        mockMvc.perform(post("/auth/register")
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

        mockMvc.perform(post("/auth/register")
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

        mockMvc.perform(post("/auth/register")
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
        mockMvc.perform(post("/auth/register")
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

        mockMvc.perform(post("/auth/register")
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

        mockMvc.perform(post("/auth/register")
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

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s"
                                }
                                """.formatted(longName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(put("/departments/{departmentId}", department.getId())
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
        mockMvc.perform(post("/auth/register")
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

        mockMvc.perform(patch("/admin/users/not-a-number/approve")
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
        MvcResult result = mockMvc.perform(post("/auth/register")
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
        MvcResult result = mockMvc.perform(post("/patents")
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
        MvcResult result = mockMvc.perform(post("/auth/login")
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
