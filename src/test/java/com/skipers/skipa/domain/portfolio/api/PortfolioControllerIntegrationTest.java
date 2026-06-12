package com.skipers.skipa.domain.portfolio.api;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.portfolio.application.PortfolioInsightCache;
import com.skipers.skipa.domain.portfolio.application.PortfolioInsightClient;
import com.skipers.skipa.domain.portfolio.dto.request.PortfolioInsightClientRequest;
import com.skipers.skipa.domain.report.application.ReportGenerationPublisher;
import com.skipers.skipa.domain.report.application.ReportService;
import com.skipers.skipa.domain.report.application.ReportStorageService;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    private ReportRepository reportRepository;

    @Autowired
    private ReviewCycleRepository reviewCycleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PortfolioInsightCache portfolioInsightCache;

    @Autowired
    private ReportService reportService;

    @MockitoBean
    private ReportGenerationPublisher reportGenerationPublisher;

    @MockitoBean
    private ReportStorageService reportStorageService;

    @MockitoBean
    private PortfolioInsightClient portfolioInsightClient;

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
        portfolioInsightCache.evict();

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
        savePatent(
                "Old Patent",
                "APP-PORT-OLD",
                "반도체",
                "KR",
                semiconductorDepartment,
                LocalDate.now().minusYears(7),
                LocalDate.now().minusYears(7),
                LocalDate.now().minusYears(7)
        );
        patentAnnuityRepository.save(PatentAnnuity.builder()
                .patent(semiconductorPatent)
                .startYear(3)
                .endYear(3)
                .dueDate(LocalDate.of(2026, 5, 1))
                .paidDate(LocalDate.of(2026, 5, 10))
                .status(PatentAnnuityStatus.PAID)
                .amount(100_000)
                .build());
        patentAnnuityRepository.save(PatentAnnuity.builder()
                .patent(batteryPatent)
                .startYear(4)
                .endYear(4)
                .dueDate(LocalDate.of(2025, 5, 1))
                .paidDate(LocalDate.of(2025, 5, 10))
                .status(PatentAnnuityStatus.PAID)
                .amount(200_000)
                .build());
        patentAnnuityRepository.save(PatentAnnuity.builder()
                .patent(batteryPatent)
                .startYear(5)
                .dueDate(LocalDate.of(2026, 7, 1))
                .status(PatentAnnuityStatus.UNPAID)
                .amount(300_000)
                .build());
        patentAnnuityRepository.save(PatentAnnuity.builder()
                .patent(batteryPatent)
                .startYear(6)
                .endYear(6)
                .dueDate(LocalDate.of(2024, 7, 1))
                .status(PatentAnnuityStatus.PAID)
                .amount(400_000)
                .build());
        patentAnnuityRepository.save(PatentAnnuity.builder()
                .patent(batteryPatent)
                .startYear(7)
                .endYear(7)
                .dueDate(LocalDate.now().minusYears(7))
                .paidDate(LocalDate.now().minusYears(7))
                .status(PatentAnnuityStatus.PAID)
                .amount(500_000)
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
        reportRepository.save(completedReport(semiconductorPatent, "S"));
        reportRepository.save(completedReport(batteryPatent, "A"));
        legalToken = createActiveUserToken("legal-portfolio", "legal-portfolio@example.com", UserRole.LEGAL);
    }

    @Test
    void portfolioInsightsReturnsAiInsightsAndCachesThem() throws Exception {
        when(portfolioInsightClient.generate(any(PortfolioInsightClientRequest.class)))
                .thenReturn(
                        List.of("인사이트 1", "인사이트 2", "인사이트 3"),
                        List.of("새 인사이트 1", "새 인사이트 2", "새 인사이트 3")
                );

        mockMvc.perform(get("/portfolio/insights")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.insights.length()").value(3))
                .andExpect(jsonPath("$.data.insights[0]").value("인사이트 1"));

        mockMvc.perform(get("/portfolio/insights")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.insights.length()").value(3))
                .andExpect(jsonPath("$.data.insights[2]").value("인사이트 3"));

        verify(portfolioInsightClient, times(1)).generate(any(PortfolioInsightClientRequest.class));

        Report generatingReport = reportRepository.save(Report.builder()
                .patent(semiconductorPatent)
                .status(ReportStatus.GENERATING)
                .build());
        reportService.complete(
                generatingReport.getId(),
                "patents/%s/reports/%s/report.json".formatted(semiconductorPatent.getId(), generatingReport.getId()),
                new BigDecimal("90.00"),
                "A"
        );

        mockMvc.perform(get("/portfolio/insights")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.insights.length()").value(3))
                .andExpect(jsonPath("$.data.insights[0]").value("새 인사이트 1"));

        verify(portfolioInsightClient, times(2)).generate(any(PortfolioInsightClientRequest.class));
    }

    @Test
    void portfolioDistributionReturnsPatentDistributions() throws Exception {
        mockMvc.perform(get("/portfolio/distribution")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.byGrade.length()").value(3))
                .andExpect(jsonPath("$.data.byGrade[0].departmentName").value("전체"))
                .andExpect(jsonPath("$.data.byGrade[0].s").value(1))
                .andExpect(jsonPath("$.data.byGrade[0].a").value(1))
                .andExpect(jsonPath("$.data.byGrade[1].departmentName").value("반도체 사업부"))
                .andExpect(jsonPath("$.data.byGrade[1].s").value(1))
                .andExpect(jsonPath("$.data.byGrade[2].departmentName").value("배터리 사업부"))
                .andExpect(jsonPath("$.data.byGrade[2].a").value(1))
                .andExpect(jsonPath("$.data.byTechField.length()").value(2))
                .andExpect(jsonPath("$.data.byFilingCountry.length()").value(2))
                .andExpect(jsonPath("$.data.byDepartment.length()").value(2));
    }

    @Test
    void portfolioTrendsReturnsYearlyPatentAndAnnuityTrends() throws Exception {
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - 6;

        mockMvc.perform(get("/portfolio/trends")
                .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.yearlyPatentTrends.length()").value(7))
                .andExpect(jsonPath("$.data.yearlyPatentTrends[0].year").value(startYear))
                .andExpect(jsonPath("$.data.yearlyPatentTrends[3].applications").value(1))
                .andExpect(jsonPath("$.data.yearlyPatentTrends[4].applications").value(1))
                .andExpect(jsonPath("$.data.yearlyPatentTrends[5].registrations").value(1))
                .andExpect(jsonPath("$.data.yearlyPatentTrends[6].year").value(currentYear))
                .andExpect(jsonPath("$.data.yearlyPatentTrends[6].expiries").value(1))
                .andExpect(jsonPath("$.data.yearlyAnnuityCosts.length()").value(7))
                .andExpect(jsonPath("$.data.yearlyAnnuityCosts[0].year").value(startYear))
                .andExpect(jsonPath("$.data.yearlyAnnuityCosts[5].amount").value(200000))
                .andExpect(jsonPath("$.data.yearlyAnnuityCosts[6].year").value(currentYear))
                .andExpect(jsonPath("$.data.yearlyAnnuityCosts[6].amount").value(100000));
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

        mockMvc.perform(get("/portfolio/insights")
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

    private Report completedReport(Patent patent, String valueGrade) {
        return Report.builder()
                .patent(patent)
                .reportKey("reports/%s/report.html".formatted(patent.getId()))
                .totalScore(valueGrade.equals("S") ? new java.math.BigDecimal("95.00") : new java.math.BigDecimal("85.00"))
                .valueGrade(valueGrade)
                .status(ReportStatus.REPORT_COMPLETED)
                .evaluatedAt(Instant.now())
                .build();
    }
}
