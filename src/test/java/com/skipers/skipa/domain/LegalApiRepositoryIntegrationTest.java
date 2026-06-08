package com.skipers.skipa.domain;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewCycleType;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class LegalApiRepositoryIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PatentRepository patentRepository;

    @Autowired
    private PatentLegalStatusRepository patentLegalStatusRepository;

    @Autowired
    private ReviewCycleRepository reviewCycleRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void reviewFiltersSupportCheckedStatusDepartmentAndPatent() {
        Department department = departmentRepository.save(Department.builder()
                .name("통신")
                .build());
        Department otherDepartment = departmentRepository.save(Department.builder()
                .name("배터리")
                .build());
        Patent patent = patentRepository.save(Patent.builder()
                .title("Checked Patent")
                .applicationNumber("APP-CHECKED")
                .currentDepartment(department)
                .build());
        Patent otherPatent = patentRepository.save(Patent.builder()
                .title("Unchecked Patent")
                .applicationNumber("APP-UNCHECKED")
                .currentDepartment(otherDepartment)
                .build());
        ReviewCycle reviewCycle = reviewCycleRepository.save(activeReviewCycle());
        Review checkedReview = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .status(ReviewStatus.SUBMITTED)
                .opinion(BusinessOpinion.MAINTAIN)
                .submittedAt(Instant.now())
                .checked(true)
                .build());
        reviewRepository.save(Review.builder()
                .patent(otherPatent)
                .department(otherDepartment)
                .reviewCycle(reviewCycle)
                .status(ReviewStatus.SUBMITTED)
                .opinion(BusinessOpinion.ABANDON)
                .submittedAt(Instant.now())
                .checked(false)
                .build());

        Page<Review> result = reviewRepository.findAllByFilters(
                ReviewStatus.SUBMITTED,
                department.getId(),
                patent.getId(),
                true,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(Review::getId)
                .containsExactly(checkedReview.getId());
    }

    @Test
    void reviewCycleLookupAndUnassignedPatentCountSupportStats() {
        Department department = departmentRepository.save(Department.builder()
                .name("반도체")
                .build());
        Patent assignedPatent = patentRepository.save(Patent.builder()
                .title("Assigned Patent")
                .applicationNumber("APP-ASSIGNED")
                .currentDepartment(department)
                .build());
        patentRepository.save(Patent.builder()
                .title("Unassigned Patent")
                .applicationNumber("APP-UNASSIGNED")
                .build());
        ReviewCycle activeCycle = reviewCycleRepository.save(activeReviewCycle());
        ReviewCycle oldCycle = reviewCycleRepository.save(ReviewCycle.builder()
                .name("지난 주기")
                .type(ReviewCycleType.QUARTERLY)
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(LocalDate.now().minusMonths(5))
                .build());
        Review activeReview = reviewRepository.save(Review.builder()
                .patent(assignedPatent)
                .department(department)
                .reviewCycle(activeCycle)
                .build());
        reviewRepository.save(Review.builder()
                .patent(assignedPatent)
                .department(department)
                .reviewCycle(oldCycle)
                .build());

        List<Review> activeReviews = reviewRepository.findAllByReviewCycleId(activeCycle.getId());

        assertThat(activeReviews)
                .extracting(Review::getId)
                .containsExactly(activeReview.getId());
        assertThat(patentRepository.countByCurrentDepartmentIsNull()).isEqualTo(1);
    }

    @Test
    void latestLegalStatusLookupUsesChangedAtThenIdDescending() {
        Patent patent = patentRepository.save(Patent.builder()
                .title("Legal Status Patent")
                .applicationNumber("APP-LEGAL-STATUS")
                .build());
        patentLegalStatusRepository.save(PatentLegalStatus.builder()
                .patent(patent)
                .status(PatentLegalStatusType.PUBLISHED)
                .changedAt(LocalDate.now().minusDays(2))
                .build());
        patentLegalStatusRepository.save(PatentLegalStatus.builder()
                .patent(patent)
                .status(PatentLegalStatusType.REGISTERED)
                .changedAt(LocalDate.now())
                .build());
        PatentLegalStatus latestStatusWithSameDate = patentLegalStatusRepository.save(PatentLegalStatus.builder()
                .patent(patent)
                .status(PatentLegalStatusType.EXPIRED)
                .changedAt(LocalDate.now())
                .build());

        PatentLegalStatus result = patentLegalStatusRepository
                .findFirstByPatentIdOrderByChangedAtDescIdDesc(patent.getId())
                .orElseThrow();

        assertThat(result.getId()).isEqualTo(latestStatusWithSameDate.getId());
        assertThat(result.getStatus()).isEqualTo(PatentLegalStatusType.EXPIRED);
    }

    private ReviewCycle activeReviewCycle() {
        return ReviewCycle.builder()
                .name("현재 주기")
                .type(ReviewCycleType.QUARTERLY)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();
    }
}
