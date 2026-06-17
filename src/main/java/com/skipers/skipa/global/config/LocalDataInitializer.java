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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private static final LocalDate SAMPLE_BASE_DATE = LocalDate.of(2026, 6, 11);

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
        Department semiconductor = departments.get(0);
        Department telecom = departments.get(1);
        Department manufacturing = departments.get(2);
        List<Department> reviewDepartments = List.of(semiconductor, telecom, manufacturing);

        List<ReviewCycle> reviewCycles = ensureReviewCycles();
        ReviewCycle currentCycle = findReviewCycle(reviewCycles, 2026, 2);
        List<ReviewCycle> pastCycles = reviewCycles.stream()
                .filter(reviewCycle -> reviewCycle.getEndDate().isBefore(LocalDate.of(2026, 6, 11)))
                .toList();

        List<Patent> patents = new ArrayList<>();
        for (int index = 1; index <= 50; index++) {
            patents.add(ensureSamplePatent(index, reviewDepartments.get((index - 1) % reviewDepartments.size())));
        }

        List<PatentLegalStatus> legalStatuses = new ArrayList<>();
        List<PatentAnnuity> annuities = new ArrayList<>();
        List<Review> reviews = new ArrayList<>();
        for (int index = 1; index <= patents.size(); index++) {
            Patent patent = patents.get(index - 1);
            Department department = patent.getCurrentDepartment();
            Report report = ensureSampleReport(index, patent);

            if (patentLegalStatusRepository.findFirstByPatentIdOrderByChangedAtDescIdDesc(patent.getId()).isEmpty()) {
                legalStatuses.add(legalStatus(patent, PatentLegalStatusType.APPLIED, LocalDate.of(2023, 1, 1).plusDays(index * 9L)));
                legalStatuses.add(legalStatus(
                        patent,
                        index % 2 == 0 ? PatentLegalStatusType.REGISTERED : PatentLegalStatusType.PUBLISHED,
                        LocalDate.of(2025, 1, 1).plusDays(index * 5L)
                ));
                PatentLegalStatusType inactiveStatus = inactiveLegalStatus(index);
                if (inactiveStatus != null) {
                    legalStatuses.add(legalStatus(patent, inactiveStatus, inactiveChangedAt(index)));
                }
            }
            PatentAnnuity annuity = ensureSampleAnnuity(index, patent, annuities);

            if (hasCurrentReview(index)
                    && !reviewRepository.existsByReviewCycleIdAndPatentIdAndDepartmentId(
                    currentCycle.getId(),
                    patent.getId(),
                    department.getId()
            )) {
                reviews.add(currentReview(index, patent, department, currentCycle, report, annuity));
            }
        }
        for (int cycleIndex = 0; cycleIndex < pastCycles.size(); cycleIndex++) {
            ReviewCycle pastCycle = pastCycles.get(cycleIndex);
            for (int offset = 0; offset < 3; offset++) {
                int patentIndex = cycleIndex * 3 + offset + 1;
                Patent patent = patents.get((patentIndex - 1) % patents.size());
                Department department = patent.getCurrentDepartment();
                if (!reviewRepository.existsByReviewCycleIdAndPatentIdAndDepartmentId(
                        pastCycle.getId(),
                        patent.getId(),
                        department.getId()
                )) {
                    Report report = reportRepository.findFirstByPatentIdAndStatusInOrderByIdDesc(
                                    patent.getId(),
                                    List.of(ReportStatus.REPORT_COMPLETED, ReportStatus.EMBEDDING_COMPLETED)
                            )
                            .orElseGet(() -> ensureSampleReport(patentIndex, patent));
                    reviews.add(previousSubmittedReview(patentIndex, patent, department, pastCycle, report));
                }
            }
        }

        patentLegalStatusRepository.saveAll(legalStatuses);
        patentAnnuityRepository.saveAll(annuities);
        reviewRepository.saveAll(reviews);

        log.info("Created {} sample patents and {} sample reviews", patents.size(), reviews.size());
    }

    private List<ReviewCycle> ensureReviewCycles() {
        List<ReviewCycle> reviewCycles = new ArrayList<>();
        for (int year = 2024; year <= 2027; year++) {
            for (int quarter = 1; quarter <= 4; quarter++) {
                reviewCycles.add(ensureReviewCycle(year, quarter));
            }
        }
        return reviewCycles;
    }

    private ReviewCycle ensureReviewCycle(int year, int quarter) {
        return reviewCycleRepository.findByYearAndQuarter(year, quarter)
                .map(reviewCycle -> {
                    reviewCycle.update(year, quarter, quarterStart(year, quarter), quarterStart(year, quarter).plusMonths(3).minusDays(1));
                    reviewCycle.updateDeadline(defaultReviewCycleDeadline(year, quarter));
                    return reviewCycle;
                })
                .orElseGet(() -> reviewCycleRepository.save(ReviewCycle.builder()
                        .year(year)
                        .quarter(quarter)
                        .startDate(quarterStart(year, quarter))
                        .endDate(quarterStart(year, quarter).plusMonths(3).minusDays(1))
                        .deadline(defaultReviewCycleDeadline(year, quarter))
                        .build()));
    }

    private ReviewCycle findReviewCycle(List<ReviewCycle> reviewCycles, int year, int quarter) {
        return reviewCycles.stream()
                .filter(reviewCycle -> reviewCycle.getYear() == year && reviewCycle.getQuarter() == quarter)
                .findFirst()
                .orElseThrow();
    }

    private LocalDate quarterStart(int year, int quarter) {
        return LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
    }

    private LocalDate defaultReviewCycleDeadline(int year, int quarter) {
        if (year == 2027 && quarter == 4) {
            return null;
        }
        return quarterStart(year, quarter).plusMonths(2).plusDays(14);
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
                .expiryDate(sampleExpiryDate(index))
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

    private Patent ensureSamplePatent(int index, Department department) {
        Patent sample = samplePatent(index, department);
        return patentRepository.findByApplicationNumber(sample.getApplicationNumber())
                .map(existing -> {
                    existing.update(
                            sample.getTitle(),
                            sample.getApplicationNumber(),
                            sample.getRegistrationNumber(),
                            sample.getPublicationNumber(),
                            sample.getAnnouncementNumber(),
                            sample.getApplicationDate(),
                            sample.getRegistrationDate(),
                            sample.getPublicationDate(),
                            sample.getAnnouncementDate(),
                            sample.getIpcCodes(),
                            sample.getCpcCodes(),
                            sample.getApplicant(),
                            sample.getInventor(),
                            sample.getExpiryDate(),
                            sample.getCitationCount(),
                            sample.getExaminationClaimCount(),
                            sample.getOriginalPdfKey(),
                            sample.getManagementNumber(),
                            sample.getBusinessField(),
                            sample.getTechField(),
                            sample.getRelatedProducts(),
                            sample.getFilingCountry(),
                            sample.getIsJointApplication(),
                            sample.getJointApplicant(),
                            sample.getInitialDepartment(),
                            sample.getKeywords(),
                            sample.getSummary()
                    );
                    existing.changeCurrentDepartment(department);
                    existing.approve();
                    return existing;
                })
                .orElseGet(() -> patentRepository.save(sample));
    }

    private Report ensureSampleReport(int index, Patent patent) {
        return reportRepository.findFirstByPatentIdAndStatusInOrderByIdDesc(
                        patent.getId(),
                        List.of(ReportStatus.REPORT_COMPLETED, ReportStatus.EMBEDDING_COMPLETED)
                )
                .orElseGet(() -> reportRepository.save(sampleCompletedReport(index, patent)));
    }

    private Report sampleCompletedReport(int index, Patent patent) {
        String grade = index % 5 == 0 ? "S" : index % 3 == 0 ? "A" : index % 3 == 1 ? "B" : "C";
        BigDecimal score = BigDecimal.valueOf(60 + index % 35).setScale(2);
        return Report.builder()
                .patent(patent)
                .reportKey("reports/sample/demo-%03d.html".formatted(index))
                .totalScore(score)
                .valueGrade(grade)
                .status(ReportStatus.EMBEDDING_COMPLETED)
                .evaluatedAt(LocalDate.of(2026, 4, 1)
                        .plusDays(index)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant())
                .build();
    }

    private PatentAnnuity ensureSampleAnnuity(int index, Patent patent, List<PatentAnnuity> pendingAnnuities) {
        return patentAnnuityRepository.findFirstByPatentIdAndStatusOrderByStartYearDescIdDesc(
                        patent.getId(),
                        PatentAnnuityStatus.UNPAID
                )
                .orElseGet(() -> {
                    PatentAnnuity sample = annuity(
                            patent,
                            1,
                            index % 3 == 0 ? 3 : null,
                            LocalDate.of(2026, 5, 15).plusDays(index % 75),
                            index % 3 == 0 ? LocalDate.of(2026, 5, 1).plusDays(index % 20) : null,
                            index % 3 == 0 ? PatentAnnuityStatus.PAID : PatentAnnuityStatus.UNPAID,
                            160000 + index * 12000
                    );
                    pendingAnnuities.add(sample);
                    return sample;
                });
    }

    private Review currentReview(
            int index,
            Patent patent,
            Department department,
            ReviewCycle currentCycle,
            Report report,
            PatentAnnuity annuity
    ) {
        ReviewStatus status = currentReviewStatus(index);
        return Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(currentCycle)
                .report(report)
                .patentAnnuity(annuity)
                .status(status)
                .opinion(status == ReviewStatus.SUBMITTED ? submittedOpinion(index) : null)
                .comment(status == ReviewStatus.SUBMITTED ? submittedComment(index) : null)
                .submittedAt(status == ReviewStatus.SUBMITTED
                        ? LocalDate.of(2026, 6, 1)
                        .plusDays(index % 10)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        : null)
                .dueDate(currentCycle.getDeadline())
                .checked(status == ReviewStatus.SUBMITTED && index % 2 == 0)
                .build();
    }

    private Review previousSubmittedReview(
            int index,
            Patent patent,
            Department department,
            ReviewCycle previousCycle,
            Report report
    ) {
        LocalDate submittedDate = previousCycle.getEndDate().minusDays(15 - index % 10);
        return Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(previousCycle)
                .report(report)
                .status(ReviewStatus.SUBMITTED)
                .opinion(index % 2 == 0 ? BusinessOpinion.MAINTAIN : BusinessOpinion.ABANDON)
                .comment("%d년 %d분기 이력 확인용 제출 의견입니다.".formatted(previousCycle.getYear(), previousCycle.getQuarter()))
                .submittedAt(submittedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                .dueDate(previousCycle.getDeadline())
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
        int submissionBucket = index % 6;
        if (submissionBucket >= 1 && submissionBucket <= 3) {
            return ReviewStatus.SUBMITTED;
        }
        return ReviewStatus.PENDING;
    }

    private boolean hasCurrentReview(int index) {
        return index % 7 != 0;
    }

    private LocalDate sampleExpiryDate(int index) {
        int offsetDays = index % 20;
        return switch ((index - 1) % 6) {
            case 0 -> SAMPLE_BASE_DATE.plusMonths(1).plusDays(offsetDays);
            case 1 -> SAMPLE_BASE_DATE.plusMonths(4).plusDays(offsetDays);
            case 2 -> SAMPLE_BASE_DATE.plusMonths(9).plusDays(offsetDays);
            case 3 -> SAMPLE_BASE_DATE.plusMonths(24).plusDays(offsetDays);
            case 4 -> SAMPLE_BASE_DATE.plusMonths(48).plusDays(offsetDays);
            default -> SAMPLE_BASE_DATE.plusMonths(72).plusDays(offsetDays);
        };
    }

    private PatentLegalStatusType inactiveLegalStatus(int index) {
        return switch (index % 8) {
            case 1 -> PatentLegalStatusType.ABANDONED;
            case 2 -> PatentLegalStatusType.EXPIRED;
            case 3 -> PatentLegalStatusType.WITHDRAWN;
            case 4 -> PatentLegalStatusType.EXPIRED;
            default -> null;
        };
    }

    private LocalDate inactiveChangedAt(int index) {
        return LocalDate.of(2022 + (index - 1) % 4, index % 12 + 1, index % 24 + 1);
    }

    private BusinessOpinion submittedOpinion(int index) {
        return index % 2 == 0 ? BusinessOpinion.MAINTAIN : BusinessOpinion.ABANDON;
    }

    private String submittedComment(int index) {
        return submittedOpinion(index) == BusinessOpinion.MAINTAIN
                ? "사업 연계성이 높아 유지가 필요합니다."
                : "대체 기술과 중복되어 포기 검토가 가능합니다.";
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
