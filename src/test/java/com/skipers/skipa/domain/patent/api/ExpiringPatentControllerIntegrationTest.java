package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.report.application.ReportGenerationPublisher;
import com.skipers.skipa.domain.report.application.ReportStorageService;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ExpiringPatentControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PatentRepository patentRepository;

    @Autowired
    private ReviewCycleRepository reviewCycleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private ReportGenerationPublisher reportGenerationPublisher;

    @MockitoBean
    private ReportStorageService reportStorageService;

    private Department semiconductorDepartment;
    private Department batteryDepartment;
    private Patent expiringSoonPatent;
    private String legalToken;
    private String businessToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        semiconductorDepartment = departmentRepository.save(Department.builder()
                .name("반도체 사업부")
                .build());
        batteryDepartment = departmentRepository.save(Department.builder()
                .name("배터리 사업부")
                .build());
        reviewCycleRepository.save(ReviewCycle.builder()
                .year(2026)
                .quarter(2)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .build());

        expiringSoonPatent = savePatent(
                "Expiring Soon Patent",
                "APP-EXPIRING-1",
                "반도체",
                semiconductorDepartment,
                LocalDate.now().plusMonths(2)
        );
        savePatent(
                "Expiring Later Patent",
                "APP-EXPIRING-2",
                "배터리",
                batteryDepartment,
                LocalDate.now().plusMonths(8)
        );
        savePatent(
                "Long Term Patent",
                "APP-EXPIRING-3",
                "소재",
                semiconductorDepartment,
                LocalDate.now().plusYears(6)
        );
        savePatent(
                "Expired Patent",
                "APP-EXPIRING-4",
                "반도체",
                semiconductorDepartment,
                LocalDate.now().minusDays(1)
        );

        legalToken = createActiveUserToken("legal-expiring", "legal-expiring@example.com", UserRole.LEGAL, null);
        businessToken = createActiveUserToken(
                "business-expiring",
                "business-expiring@example.com",
                UserRole.BUSINESS,
                semiconductorDepartment
        );
    }

    @Test
    void expiringSummaryReturnsPeriodCountsAndTechFieldBreakdown() throws Exception {
        mockMvc.perform(get("/patents/expiring/summary")
                .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periods.length()").value(5))
                .andExpect(jsonPath("$.data.periods[0].period").doesNotExist())
                .andExpect(jsonPath("$.data.periods[0].months").value(3))
                .andExpect(jsonPath("$.data.periods[0].byTechField.length()").value(1))
                .andExpect(jsonPath("$.data.periods[0].byTechField[0].name").value("반도체"))
                .andExpect(jsonPath("$.data.periods[0].byTechField[0].count").value(1))
                .andExpect(jsonPath("$.data.periods[2].months").value(12))
                .andExpect(jsonPath("$.data.periods[2].byTechField.length()").value(2))
                .andExpect(jsonPath("$.data.periods[4].months").value(60));
    }

    @Test
    void expiringPatentListReturnsSelectedPeriodItems() throws Exception {
        mockMvc.perform(get("/patents/expiring")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(expiringSoonPatent.getId()))
                .andExpect(jsonPath("$.data.items[0].departmentName").value("반도체 사업부"));

        mockMvc.perform(get("/patents/expiring")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("months", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(expiringSoonPatent.getId()));
    }

    @Test
    void expiringPatentListRejectsInvalidMonths() throws Exception {
        mockMvc.perform(get("/patents/expiring")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("months", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void expiringCalendarGroupsItemsByYearAndMonth() throws Exception {
        mockMvc.perform(get("/patents/expiring/calendar")
                        .header("Authorization", "Bearer " + legalToken)
                        .param("year", String.valueOf(LocalDate.now().getYear())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.months.length()").value(12))
                .andExpect(jsonPath("$.data.months[%d].count".formatted(LocalDate.now().plusMonths(2).getMonthValue() - 1)).value(1));
    }

    @Test
    void businessUserOnlySeesOwnDepartmentExpiringPatents() throws Exception {
        mockMvc.perform(get("/patents/expiring")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(expiringSoonPatent.getId()));

        mockMvc.perform(get("/patents/expiring/calendar")
                        .header("Authorization", "Bearer " + businessToken)
                        .param("year", String.valueOf(LocalDate.now().getYear())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.months.length()").value(12))
                .andExpect(jsonPath("$.data.months[%d].count".formatted(LocalDate.now().plusMonths(2).getMonthValue() - 1)).value(1));
    }

    private Patent savePatent(
            String title,
            String applicationNumber,
            String techField,
            Department department,
            LocalDate expiryDate
    ) {
        return patentRepository.save(Patent.builder()
                .title(title)
                .applicationNumber(applicationNumber)
                .techField(techField)
                .filingCountry("KR")
                .currentDepartment(department)
                .expiryDate(expiryDate)
                .build());
    }

    private String createActiveUserToken(String loginId, String email, UserRole role, Department department) {
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
