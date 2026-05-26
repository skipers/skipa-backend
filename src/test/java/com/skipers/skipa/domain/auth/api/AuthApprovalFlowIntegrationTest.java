package com.skipers.skipa.domain.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private Department department;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        department = departmentRepository.save(Department.builder()
                .name("통신")
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.departmentId").value(nullValue()))
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
