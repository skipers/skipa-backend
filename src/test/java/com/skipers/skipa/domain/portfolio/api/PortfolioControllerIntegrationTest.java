package com.skipers.skipa.domain.portfolio.api;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.report.application.ReportGenerationPublisher;
import com.skipers.skipa.domain.report.application.ReportStorageService;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
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

import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class PortfolioControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PatentRepository patentRepository;

    @Autowired
    private PatentAnnuityRepository patentAnnuityRepository;

    @Autowired
    private ReviewRepository reviewRepository;

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
    private ReviewCycle reviewCycle;
    private Patent semiconductorPatent;
    private Patent batteryPatent;
    private String legalToken;

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
        reviewCycle = reviewCycleRepository.save(ReviewCycle.builder()
                .year(2026)
                .quarter(2)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .build());

        semiconductorPatent = savePatent(
                "Semiconductor Patent",
                "APP-PORT-1",
                "반도체",
                "KR",
                semiconductorDepartment,
                LocalDate.of(2023, 1, 10),
                LocalDate.of(2025, 3, 15),
                LocalDate.now().plusMonths(6)
        );
        batteryPatent = savePatent(
                "Battery Patent",
                "APP-PORT-2",
                "배터리",
                "US",
                batteryDepartment,
                LocalDate.of(2024, 2, 20),
                null,
                LocalDate.now().plusYears(2)
        );
        patentAnnuityRepository.save(PatentAnnuity.builder()
                .patent(semiconductorPatent)
                .annuityYear(3)
                .dueDate(LocalDate.of(2026, 5, 1))
                .status(PatentAnnuityStatus.PAID)
                .amount(100_000)
                .build());
        reviewRepository.save(Review.builder()
                .patent(semiconductorPatent)
                .department(semiconductorDepartment)
                .reviewCycle(reviewCycle)
                .opinion(BusinessOpinion.MAINTAIN)
                .status(ReviewStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build());
        reviewRepository.save(Review.builder()
                .patent(batteryPatent)
                .department(batteryDepartment)
                .reviewCycle(reviewCycle)
                .opinion(BusinessOpinion.ABANDON)
                .status(ReviewStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build());
        legalToken = createActiveUserToken("legal-portfolio", "legal-portfolio@example.com", UserRole.LEGAL);
    }

    @Test
    void portfolioSummaryReturnsCoreMetrics() throws Exception {
        mockMvc.perform(get("/portfolio/summary")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPatents").value(2))
                .andExpect(jsonPath("$.data.expiringWithinYear").value(1))
                .andExpect(jsonPath("$.data.countryCount").value(2))
                .andExpect(jsonPath("$.data.techFieldCount").value(2))
                .andExpect(jsonPath("$.data.insights.length()").value(3));
    }

    @Test
    void portfolioDistributionReturnsPatentDistributions() throws Exception {
        mockMvc.perform(get("/portfolio/distribution")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.byValueGrade.length()").value(0))
                .andExpect(jsonPath("$.data.byTechField.length()").value(2))
                .andExpect(jsonPath("$.data.byFilingCountry.length()").value(2))
                .andExpect(jsonPath("$.data.byDepartment.length()").value(2));
    }

    @Test
    void portfolioTrendsReturnsYearlyPatentAndAnnuityTrends() throws Exception {
        mockMvc.perform(get("/portfolio/trends")
                .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.yearlyPatentTrends.length()").value(5))
                .andExpect(jsonPath("$.data.yearlyAnnuityCosts[0].year").value(2026))
                .andExpect(jsonPath("$.data.yearlyAnnuityCosts[0].amount").value(100000));
    }

    @Test
    void portfolioDecisionsReturnsDecisionBreakdowns() throws Exception {
        mockMvc.perform(get("/portfolio/decisions")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.byQuarter.length()").value(1))
                .andExpect(jsonPath("$.data.byQuarter[0].maintain").value(1))
                .andExpect(jsonPath("$.data.byQuarter[0].abandon").value(1))
                .andExpect(jsonPath("$.data.byDepartment.length()").value(2))
                .andExpect(jsonPath("$.data.byTechField.length()").value(2));
    }

    @Test
    void portfolioApisForbidBusinessUsers() throws Exception {
        String businessToken = createActiveUserToken("business-portfolio", "business-portfolio@example.com", UserRole.BUSINESS);

        mockMvc.perform(get("/portfolio/summary")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isForbidden());
    }

    private Patent savePatent(
            String title,
            String applicationNumber,
            String techField,
            String filingCountry,
            Department department,
            LocalDate applicationDate,
            LocalDate registrationDate,
            LocalDate expiryDate
    ) {
        return patentRepository.save(Patent.builder()
                .title(title)
                .applicationNumber(applicationNumber)
                .techField(techField)
                .filingCountry(filingCountry)
                .currentDepartment(department)
                .applicationDate(applicationDate)
                .registrationDate(registrationDate)
                .expiryDate(expiryDate)
                .build());
    }

    private String createActiveUserToken(String loginId, String email, UserRole role) {
        User user = userRepository.save(User.createActive(
                loginId,
                loginId,
                email,
                passwordEncoder.encode("password"),
                role,
                role == UserRole.BUSINESS ? semiconductorDepartment : null
        ));
        return jwtProvider.createAccessToken(user.getId(), role);
    }
}
