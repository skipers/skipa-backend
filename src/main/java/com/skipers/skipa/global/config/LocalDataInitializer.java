package com.skipers.skipa.global.config;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.patent.domain.PatentApprovalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatentRepository patentRepository;
    private final PatentLegalStatusRepository patentLegalStatusRepository;
    private final PatentAnnuityRepository patentAnnuityRepository;
    private final ReviewCycleRepository reviewCycleRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;

    @Value("${app.local.seed.password}")
    private String seedPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Department> departments = ensureDepartments();
        ensureUsers(departments);
        ensureBusinessSampleData(departments);
    }

    private List<Department> ensureDepartments() {
        Department semiconductor = ensureDepartment("반도체");
        Department telecom = ensureDepartment("통신");
        Department manufacturing = ensureDepartment("제조");
        return List.of(semiconductor, telecom, manufacturing);
    }

    private Department ensureDepartment(String name) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> departmentRepository.save(Department.builder().name(name).build()));
    }

    private void ensureUsers(List<Department> departments) {
        if (userRepository.count() > 0) {
            return;
        }

        Department semiconductor = departments.get(0);
        Department telecom = departments.get(1);
        Department manufacturing = departments.get(2);

        String encodedPassword = passwordEncoder.encode(seedPassword);
        List<User> seedUsers = List.of(
                User.createActive("admin", "관리자", "admin@sk.com", encodedPassword, UserRole.ADMIN, null),
                User.createActive("legal01", "법무 담당자 1", "legal01@sk.com", encodedPassword, UserRole.LEGAL, null),
                User.createActive("legal02", "법무 담당자 2", "legal02@sk.com", encodedPassword, UserRole.LEGAL, null),
                User.createActive("legal03", "법무 담당자 3", "legal03@sk.com", encodedPassword, UserRole.LEGAL, null),
                User.createActive("legal04", "법무 담당자 4", "legal04@sk.com", encodedPassword, UserRole.LEGAL, null),
                User.createActive("business01", "사업부 담당자 1", "business01@sk.com", encodedPassword, UserRole.BUSINESS, semiconductor),
                User.createActive("business02", "사업부 담당자 2", "business02@sk.com", encodedPassword, UserRole.BUSINESS, semiconductor),
                User.createActive("business03", "사업부 담당자 3", "business03@sk.com", encodedPassword, UserRole.BUSINESS, telecom),
                User.createActive("business04", "사업부 담당자 4", "business04@sk.com", encodedPassword, UserRole.BUSINESS, manufacturing),
                User.createActive("business05", "사업부 담당자 5", "business05@sk.com", encodedPassword, UserRole.BUSINESS, manufacturing)
        );

        userRepository.saveAll(seedUsers);
        log.info("Created {} seed accounts", seedUsers.size());
    }

    private void ensureBusinessSampleData(List<Department> departments) {
        if (patentRepository.count() > 0) {
            return;
        }

        Department semiconductor = departments.get(0);
        Department telecom = departments.get(1);
        Department manufacturing = departments.get(2);

        ReviewCycle currentCycle = reviewCycleRepository.findByYearAndQuarter(2026, 2)
                .orElseGet(() -> reviewCycleRepository.save(ReviewCycle.builder()
                        .year(2026)
                        .quarter(2)
                        .startDate(LocalDate.of(2026, 4, 1))
                        .endDate(LocalDate.of(2026, 6, 30))
                        .build()));
        ReviewCycle previousCycle = reviewCycleRepository.findByYearAndQuarter(2026, 1)
                .orElseGet(() -> reviewCycleRepository.save(ReviewCycle.builder()
                        .year(2026)
                        .quarter(1)
                        .startDate(LocalDate.of(2026, 1, 1))
                        .endDate(LocalDate.of(2026, 3, 31))
                        .build()));

        Patent edgeAi = Patent.builder()
                .title("엣지 AI 반도체 전력 최적화 장치")
                .applicationNumber("10-2026-000101")
                .registrationNumber("10-2600101")
                .publicationNumber("10-2026-700101")
                .applicationDate(LocalDate.of(2024, 3, 12))
                .registrationDate(LocalDate.of(2026, 2, 20))
                .expiryDate(LocalDate.of(2044, 3, 12))
                .ipcCodes(List.of("G06N 3/08", "H01L 23/00"))
                .cpcCodes(List.of("G06N3/084", "H01L23/528"))
                .applicant("SK하이닉스")
                .inventor("김서준; 박민아")
                .citationCount(18)
                .examinationClaimCount(12)
                .managementNumber("SKP-SEM-001")
                .businessField("AI 반도체")
                .techField("저전력 추론 가속")
                .relatedProducts(List.of("HBM", "Edge AI SoC"))
                .filingCountry("KR")
                .isJointApplication(false)
                .initialDepartment("반도체")
                .currentDepartment(semiconductor)
                .keywords(List.of("AI", "전력 최적화", "반도체"))
                .summary("엣지 AI 칩의 연산 부하에 따라 전력 도메인을 동적으로 조절하는 기술입니다.")
                .approvalStatus(PatentApprovalStatus.APPROVED)
                .build();
        Patent rfBeam = Patent.builder()
                .title("5G/6G 기지국 빔포밍 캘리브레이션 방법")
                .applicationNumber("10-2026-000102")
                .publicationNumber("10-2026-700102")
                .applicationDate(LocalDate.of(2023, 11, 3))
                .expiryDate(LocalDate.of(2043, 11, 3))
                .ipcCodes(List.of("H04B 7/06", "H04W 16/28"))
                .cpcCodes(List.of("H04B7/0617", "H04W16/28"))
                .applicant("SK텔레콤")
                .inventor("이도윤; 최하린")
                .citationCount(9)
                .examinationClaimCount(8)
                .managementNumber("SKP-TEL-001")
                .businessField("무선 네트워크")
                .techField("빔포밍")
                .relatedProducts(List.of("5G DU", "6G Massive MIMO"))
                .filingCountry("KR")
                .isJointApplication(true)
                .jointApplicant("SK스퀘어")
                .initialDepartment("통신")
                .currentDepartment(telecom)
                .keywords(List.of("5G", "6G", "빔포밍"))
                .summary("다중 안테나 환경에서 캘리브레이션 시간을 줄이고 빔 정확도를 높이는 방법입니다.")
                .approvalStatus(PatentApprovalStatus.APPROVED)
                .build();
        Patent smartFactory = Patent.builder()
                .title("스마트팩토리 예지보전 센서 데이터 처리 시스템")
                .applicationNumber("10-2026-000103")
                .registrationNumber("10-2600103")
                .applicationDate(LocalDate.of(2022, 8, 19))
                .registrationDate(LocalDate.of(2025, 10, 7))
                .expiryDate(LocalDate.of(2042, 8, 19))
                .ipcCodes(List.of("G05B 23/02", "G06Q 10/20"))
                .cpcCodes(List.of("G05B23/0243", "G06Q10/20"))
                .applicant("SK온")
                .inventor("정유진; 한지훈")
                .citationCount(27)
                .examinationClaimCount(15)
                .managementNumber("SKP-MFG-001")
                .businessField("제조 DX")
                .techField("예지보전")
                .relatedProducts(List.of("배터리 생산라인", "MES"))
                .filingCountry("KR")
                .isJointApplication(false)
                .initialDepartment("제조")
                .currentDepartment(manufacturing)
                .keywords(List.of("스마트팩토리", "센서", "예지보전"))
                .summary("설비 센서 스트림을 분석해 이상 징후와 정비 시점을 예측하는 시스템입니다.")
                .approvalStatus(PatentApprovalStatus.APPROVED)
                .build();
        Patent memoryPackage = Patent.builder()
                .title("고대역폭 메모리 패키지 방열 구조")
                .applicationNumber("10-2026-000104")
                .applicationDate(LocalDate.of(2024, 6, 24))
                .expiryDate(LocalDate.of(2044, 6, 24))
                .ipcCodes(List.of("H01L 23/34"))
                .cpcCodes(List.of("H01L23/367"))
                .applicant("SK하이닉스")
                .inventor("문태오")
                .citationCount(4)
                .examinationClaimCount(6)
                .managementNumber("SKP-SEM-002")
                .businessField("메모리")
                .techField("패키징 방열")
                .relatedProducts(List.of("HBM4"))
                .filingCountry("US")
                .isJointApplication(false)
                .initialDepartment("반도체")
                .currentDepartment(semiconductor)
                .keywords(List.of("HBM", "방열", "패키지"))
                .summary("고대역폭 메모리 적층 구조에서 열 확산 경로를 개선하는 방열 구조입니다.")
                .approvalStatus(PatentApprovalStatus.APPROVED)
                .build();
        Patent pendingPatent = Patent.builder()
                .title("사업부 등록 승인 대기 특허 샘플")
                .applicationNumber("10-2026-000105")
                .applicationDate(LocalDate.of(2026, 5, 2))
                .expiryDate(LocalDate.of(2046, 5, 2))
                .applicant("SK이노베이션")
                .inventor("오지민")
                .citationCount(0)
                .examinationClaimCount(5)
                .managementNumber("SKP-PENDING-001")
                .businessField("배터리")
                .techField("전극 공정")
                .relatedProducts(List.of("배터리 셀"))
                .filingCountry("KR")
                .isJointApplication(false)
                .initialDepartment("제조")
                .currentDepartment(manufacturing)
                .keywords(List.of("승인대기", "배터리"))
                .summary("사업부가 직접 등록해 Legal 승인 대기 상태인 특허 샘플입니다.")
                .approvalStatus(PatentApprovalStatus.PENDING_APPROVAL)
                .build();

        List<Patent> patents = patentRepository.saveAll(List.of(
                edgeAi,
                rfBeam,
                smartFactory,
                memoryPackage,
                pendingPatent
        ));

        reportRepository.saveAll(List.of(
                completedReport(edgeAi, "reports/sample/edge-ai.html", "92.50", "S", LocalDate.of(2026, 4, 18)),
                completedReport(rfBeam, "reports/sample/rf-beam.html", "84.00", "A", LocalDate.of(2026, 4, 22)),
                completedReport(smartFactory, "reports/sample/smart-factory.html", "71.25", "B", LocalDate.of(2026, 5, 6)),
                Report.builder()
                        .patent(memoryPackage)
                        .status(ReportStatus.GENERATING)
                        .build(),
                Report.builder()
                        .patent(pendingPatent)
                        .status(ReportStatus.FAILED)
                        .build()
        ));

        patentLegalStatusRepository.saveAll(List.of(
                legalStatus(edgeAi, PatentLegalStatusType.APPLIED, LocalDate.of(2024, 3, 12)),
                legalStatus(edgeAi, PatentLegalStatusType.REGISTERED, LocalDate.of(2026, 2, 20)),
                legalStatus(rfBeam, PatentLegalStatusType.APPLIED, LocalDate.of(2023, 11, 3)),
                legalStatus(rfBeam, PatentLegalStatusType.PUBLISHED, LocalDate.of(2025, 5, 3)),
                legalStatus(smartFactory, PatentLegalStatusType.APPLIED, LocalDate.of(2022, 8, 19)),
                legalStatus(smartFactory, PatentLegalStatusType.REGISTERED, LocalDate.of(2025, 10, 7)),
                legalStatus(memoryPackage, PatentLegalStatusType.APPLIED, LocalDate.of(2024, 6, 24)),
                legalStatus(pendingPatent, PatentLegalStatusType.APPLIED, LocalDate.of(2026, 5, 2))
        ));

        patentAnnuityRepository.saveAll(List.of(
                annuity(edgeAi, 1, 3, LocalDate.of(2026, 7, 31), null, PatentAnnuityStatus.UNPAID, 420000),
                annuity(rfBeam, 1, 2, LocalDate.of(2026, 5, 31), LocalDate.of(2026, 5, 20), PatentAnnuityStatus.PAID, 280000),
                annuity(smartFactory, 1, 4, LocalDate.of(2026, 6, 5), null, PatentAnnuityStatus.UNPAID, 510000),
                annuity(memoryPackage, 1, null, LocalDate.of(2026, 8, 12), null, PatentAnnuityStatus.UNPAID, 190000)
        ));

        Review submittedReview = Review.builder()
                .patent(rfBeam)
                .department(telecom)
                .reviewCycle(currentCycle)
                .status(ReviewStatus.SUBMITTED)
                .opinion(BusinessOpinion.MAINTAIN)
                .comment("망 고도화 로드맵과 직접 연관되어 유지가 필요합니다.")
                .submittedAt(Instant.parse("2026-06-10T02:15:00Z"))
                .dueDate(LocalDate.of(2026, 6, 20))
                .build();
        Review previousSubmittedReview = Review.builder()
                .patent(edgeAi)
                .department(semiconductor)
                .reviewCycle(previousCycle)
                .status(ReviewStatus.SUBMITTED)
                .opinion(BusinessOpinion.ABANDON)
                .comment("대체 출원과 권리범위가 중복되어 포기 의견입니다.")
                .submittedAt(Instant.parse("2026-03-15T05:30:00Z"))
                .dueDate(LocalDate.of(2026, 3, 20))
                .checked(true)
                .build();

        reviewRepository.saveAll(List.of(
                Review.builder()
                        .patent(edgeAi)
                        .department(semiconductor)
                        .reviewCycle(currentCycle)
                        .status(ReviewStatus.PENDING)
                        .dueDate(LocalDate.of(2026, 6, 24))
                        .build(),
                submittedReview,
                Review.builder()
                        .patent(smartFactory)
                        .department(manufacturing)
                        .reviewCycle(currentCycle)
                        .status(ReviewStatus.OVERDUE)
                        .dueDate(LocalDate.of(2026, 6, 5))
                        .build(),
                Review.builder()
                        .patent(memoryPackage)
                        .department(semiconductor)
                        .reviewCycle(currentCycle)
                        .status(ReviewStatus.SCHEDULED)
                        .dueDate(LocalDate.of(2026, 6, 26))
                        .build(),
                previousSubmittedReview
        ));

        log.info("Created {} sample patents, 2 review cycles including 2026-Q2, and review sample data", patents.size());
    }

    private Report completedReport(Patent patent, String reportKey, String score, String grade, LocalDate evaluatedDate) {
        return Report.builder()
                .patent(patent)
                .reportKey(reportKey)
                .totalScore(new BigDecimal(score))
                .valueGrade(grade)
                .status(ReportStatus.COMPLETED)
                .evaluatedAt(evaluatedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }

    private PatentLegalStatus legalStatus(Patent patent, PatentLegalStatusType status, LocalDate changedAt) {
        return PatentLegalStatus.builder()
                .patent(patent)
                .status(status)
                .changedAt(changedAt)
                .build();
    }

    private PatentAnnuity annuity(
            Patent patent,
            Integer startYear,
            Integer endYear,
            LocalDate dueDate,
            LocalDate paidDate,
            PatentAnnuityStatus status,
            Integer amount
    ) {
        return PatentAnnuity.builder()
                .patent(patent)
                .startYear(startYear)
                .endYear(endYear)
                .dueDate(dueDate)
                .paidDate(paidDate)
                .status(status)
                .amount(amount)
                .build();
    }
}
