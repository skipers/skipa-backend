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
import java.util.ArrayList;
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
                User.createActive("biz01", "사업부 담당자 1", "biz01@sk.com", encodedPassword, UserRole.BUSINESS, semiconductor),
                User.createActive("biz02", "사업부 담당자 2", "biz02@sk.com", encodedPassword, UserRole.BUSINESS, semiconductor),
                User.createActive("biz03", "사업부 담당자 3", "biz03@sk.com", encodedPassword, UserRole.BUSINESS, telecom),
                User.createActive("biz04", "사업부 담당자 4", "biz04@sk.com", encodedPassword, UserRole.BUSINESS, manufacturing),
                User.createActive("biz05", "사업부 담당자 5", "biz05@sk.com", encodedPassword, UserRole.BUSINESS, manufacturing)
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
        List<Department> reviewDepartments = List.of(semiconductor, telecom, manufacturing);

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

        List<Patent> patents = new ArrayList<>();
        for (int index = 1; index <= 50; index++) {
            patents.add(samplePatent(index, reviewDepartments.get((index - 1) % reviewDepartments.size())));
        }
        patents = patentRepository.saveAll(patents);

        List<Report> reports = new ArrayList<>();
        List<PatentLegalStatus> legalStatuses = new ArrayList<>();
        List<PatentAnnuity> annuities = new ArrayList<>();
        List<Review> reviews = new ArrayList<>();
        for (int index = 1; index <= patents.size(); index++) {
            Patent patent = patents.get(index - 1);
            Department department = patent.getCurrentDepartment();

            reports.add(sampleReport(index, patent));
            legalStatuses.add(legalStatus(patent, PatentLegalStatusType.APPLIED, LocalDate.of(2023, 1, 1).plusDays(index * 9L)));
            legalStatuses.add(legalStatus(
                    patent,
                    index % 2 == 0 ? PatentLegalStatusType.REGISTERED : PatentLegalStatusType.PUBLISHED,
                    LocalDate.of(2025, 1, 1).plusDays(index * 5L)
            ));
            annuities.add(annuity(
                    patent,
                    1,
                    index % 3 == 0 ? 3 : null,
                    LocalDate.of(2026, 5, 15).plusDays(index % 75),
                    index % 3 == 0 ? LocalDate.of(2026, 5, 1).plusDays(index % 20) : null,
                    index % 3 == 0 ? PatentAnnuityStatus.PAID : PatentAnnuityStatus.UNPAID,
                    160000 + index * 12000
            ));

            reviews.add(currentReview(index, patent, department, currentCycle));
            if (index <= 10) {
                reviews.add(previousSubmittedReview(index, patent, department, previousCycle));
            }
        }

        reportRepository.saveAll(reports);
        patentLegalStatusRepository.saveAll(legalStatuses);
        patentAnnuityRepository.saveAll(annuities);
        reviewRepository.saveAll(reviews);

        log.info("Created {} sample patents and {} sample reviews", patents.size(), reviews.size());
    }

    private Patent samplePatent(int index, Department department) {
        String sequence = "%03d".formatted(index);
        String departmentName = department.getName();
        String title = switch (departmentName) {
            case "반도체" -> "AI 반도체 전력 최적화 샘플 특허 " + sequence;
            case "통신" -> "5G/6G 네트워크 제어 샘플 특허 " + sequence;
            default -> "스마트팩토리 예지보전 샘플 특허 " + sequence;
        };
        String businessField = switch (departmentName) {
            case "반도체" -> "AI 반도체";
            case "통신" -> "무선 네트워크";
            default -> "제조 DX";
        };
        String techField = switch (departmentName) {
            case "반도체" -> index % 2 == 0 ? "패키징 방열" : "저전력 추론 가속";
            case "통신" -> index % 2 == 0 ? "빔포밍" : "네트워크 슬라이싱";
            default -> index % 2 == 0 ? "예지보전" : "공정 최적화";
        };
        List<String> keywords = switch (departmentName) {
            case "반도체" -> List.of("AI", "반도체", techField);
            case "통신" -> List.of("5G", "6G", techField);
            default -> List.of("스마트팩토리", "센서", techField);
        };

        return Patent.builder()
                .title(title)
                .applicationNumber("10-2026-%06d".formatted(100000 + index))
                .registrationNumber(index % 2 == 0 ? "10-26%06d".formatted(index) : null)
                .publicationNumber("10-2026-%06d".formatted(700000 + index))
                .applicationDate(LocalDate.of(2022 + index % 4, index % 12 + 1, index % 24 + 1))
                .registrationDate(index % 2 == 0 ? LocalDate.of(2025, index % 12 + 1, index % 24 + 1) : null)
                .publicationDate(LocalDate.of(2024, index % 12 + 1, index % 24 + 1))
                .expiryDate(LocalDate.of(2042 + index % 4, index % 12 + 1, index % 24 + 1))
                .ipcCodes(List.of(index % 2 == 0 ? "G06N 3/08" : "H04B 7/06", "G05B 23/02"))
                .cpcCodes(List.of(index % 2 == 0 ? "G06N3/084" : "H04B7/0617", "G05B23/0243"))
                .applicant(index % 3 == 0 ? "SK텔레콤" : index % 3 == 1 ? "SK하이닉스" : "SK온")
                .inventor("샘플 발명자 %02d; 공동 발명자 %02d".formatted(index, index + 50))
                .citationCount(index * 3 % 41)
                .examinationClaimCount(5 + index % 12)
                .managementNumber("SKP-DEMO-%03d".formatted(index))
                .businessField(businessField)
                .techField(techField)
                .relatedProducts(List.of(businessField + " 제품군", "Demo Product " + sequence))
                .filingCountry(index % 5 == 0 ? "US" : "KR")
                .isJointApplication(index % 7 == 0)
                .jointApplicant(index % 7 == 0 ? "SK스퀘어" : null)
                .initialDepartment(departmentName)
                .currentDepartment(department)
                .keywords(keywords)
                .summary("%s의 화면 검증을 위해 생성된 샘플 특허입니다. 검토, 보고서, 권리 상태, 연차료 데이터를 함께 가집니다.".formatted(departmentName))
                .approvalStatus(PatentApprovalStatus.APPROVED)
                .build();
    }

    private Report sampleReport(int index, Patent patent) {
        if (index % 15 == 0) {
            return Report.builder()
                    .patent(patent)
                    .status(ReportStatus.FAILED)
                    .build();
        }
        if (index % 10 == 0) {
            return Report.builder()
                    .patent(patent)
                    .status(ReportStatus.GENERATING)
                    .build();
        }

        String grade = index % 5 == 0 ? "S" : index % 3 == 0 ? "A" : index % 3 == 1 ? "B" : "C";
        BigDecimal score = BigDecimal.valueOf(60 + index % 35).setScale(2);
        return Report.builder()
                .patent(patent)
                .reportKey("reports/sample/demo-%03d.html".formatted(index))
                .totalScore(score)
                .valueGrade(grade)
                .status(ReportStatus.COMPLETED)
                .evaluatedAt(LocalDate.of(2026, 4, 1)
                        .plusDays(index)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant())
                .build();
    }

    private Review currentReview(int index, Patent patent, Department department, ReviewCycle currentCycle) {
        ReviewStatus status = currentReviewStatus(index);
        return Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(currentCycle)
                .status(status)
                .opinion(status == ReviewStatus.SUBMITTED ? submittedOpinion(index) : null)
                .comment(status == ReviewStatus.SUBMITTED ? submittedComment(index) : null)
                .submittedAt(status == ReviewStatus.SUBMITTED
                        ? LocalDate.of(2026, 6, 1)
                        .plusDays(index % 10)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        : null)
                .dueDate(currentDueDate(index, status))
                .checked(status == ReviewStatus.SUBMITTED && index % 2 == 0)
                .build();
    }

    private Review previousSubmittedReview(int index, Patent patent, Department department, ReviewCycle previousCycle) {
        return Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(previousCycle)
                .status(ReviewStatus.SUBMITTED)
                .opinion(index % 2 == 0 ? BusinessOpinion.MAINTAIN : BusinessOpinion.ABANDON)
                .comment("2026년 1분기 이력 확인용 제출 의견입니다.")
                .submittedAt(Instant.parse("2026-03-%02dT02:30:00Z".formatted(10 + index)))
                .dueDate(LocalDate.of(2026, 3, 20))
                .checked(true)
                .build();
    }

    private ReviewStatus currentReviewStatus(int index) {
        if (index % 5 == 0) {
            return ReviewStatus.SCHEDULED;
        }
        if (index % 4 == 0) {
            return ReviewStatus.OVERDUE;
        }
        if (index % 3 == 0) {
            return ReviewStatus.SUBMITTED;
        }
        return ReviewStatus.PENDING;
    }

    private BusinessOpinion submittedOpinion(int index) {
        return index % 2 == 0 ? BusinessOpinion.MAINTAIN : BusinessOpinion.ABANDON;
    }

    private String submittedComment(int index) {
        return submittedOpinion(index) == BusinessOpinion.MAINTAIN
                ? "사업 연계성이 높아 유지가 필요합니다."
                : "대체 기술과 중복되어 포기 검토가 가능합니다.";
    }

    private LocalDate currentDueDate(int index, ReviewStatus status) {
        return switch (status) {
            case OVERDUE -> LocalDate.of(2026, 6, 1).plusDays(index % 5);
            case SCHEDULED -> LocalDate.of(2026, 6, 26);
            default -> LocalDate.of(2026, 6, 15).plusDays(index % 12);
        };
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
