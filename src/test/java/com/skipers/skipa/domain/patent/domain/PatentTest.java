package com.skipers.skipa.domain.patent.domain;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dto.response.PatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.PatentListResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PatentTest {

    @Test
    void updateReplacesEveryMutablePatentField() {
        Patent patent = Patent.builder()
                .title("Old")
                .applicationNumber("OLD-APP")
                .build();

        patent.update(
                "Updated",
                "APP-2",
                "REG-2",
                "PUB-2",
                "ANN-2",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4),
                "IPC",
                "CPC",
                "Applicant",
                "Inventor",
                LocalDate.of(2046, 1, 1),
                3,
                "pdf-key",
                "management",
                "business",
                "tech",
                "[\"Product\"]",
                "KR",
                true,
                "Joint Applicant",
                "Initial Department",
                "[\"Keyword\"]",
                "Overview",
                "Core Content"
        );

        assertThat(patent.getTitle()).isEqualTo("Updated");
        assertThat(patent.getApplicationNumber()).isEqualTo("APP-2");
        assertThat(patent.getRegistrationNumber()).isEqualTo("REG-2");
        assertThat(patent.getPublicationNumber()).isEqualTo("PUB-2");
        assertThat(patent.getAnnouncementNumber()).isEqualTo("ANN-2");
        assertThat(patent.getApplicationDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(patent.getRegistrationDate()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(patent.getPublicationDate()).isEqualTo(LocalDate.of(2026, 1, 3));
        assertThat(patent.getAnnouncementDate()).isEqualTo(LocalDate.of(2026, 1, 4));
        assertThat(patent.getIpcCode()).isEqualTo("IPC");
        assertThat(patent.getCpcCode()).isEqualTo("CPC");
        assertThat(patent.getApplicant()).isEqualTo("Applicant");
        assertThat(patent.getInventor()).isEqualTo("Inventor");
        assertThat(patent.getExpiryDate()).isEqualTo(LocalDate.of(2046, 1, 1));
        assertThat(patent.getCitationCount()).isEqualTo(3);
        assertThat(patent.getOriginalPdfKey()).isEqualTo("pdf-key");
        assertThat(patent.getManagementNumber()).isEqualTo("management");
        assertThat(patent.getBusinessField()).isEqualTo("business");
        assertThat(patent.getTechField()).isEqualTo("tech");
        assertThat(patent.getRelatedProducts()).isEqualTo("[\"Product\"]");
        assertThat(patent.getFilingCountry()).isEqualTo("KR");
        assertThat(patent.getIsJointApplication()).isTrue();
        assertThat(patent.getJointApplicant()).isEqualTo("Joint Applicant");
        assertThat(patent.getInitialDepartment()).isEqualTo("Initial Department");
        assertThat(patent.getKeywords()).isEqualTo("[\"Keyword\"]");
        assertThat(patent.getOverview()).isEqualTo("Overview");
        assertThat(patent.getCoreContent()).isEqualTo("Core Content");
    }

    @Test
    void responseFactoriesMapPatentValuesAndDecodedLists() {
        Department currentDepartment = Department.builder().name("Legal").build();
        Patent patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .applicant("Applicant")
                .inventor("Inventor")
                .initialDepartment("Legal")
                .currentDepartment(currentDepartment)
                .build();
        Instant createdAt = Instant.parse("2026-05-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-05-02T00:00:00Z");
        ReflectionTestUtils.setField(patent, "id", 1L);
        ReflectionTestUtils.setField(patent, "createdAt", createdAt);
        ReflectionTestUtils.setField(patent, "updatedAt", updatedAt);
        ReflectionTestUtils.setField(currentDepartment, "id", 10L);

        PatentDetailResponse detailResponse = PatentDetailResponse.from(
                patent,
                List.of("Product"),
                List.of("Keyword")
        );
        PatentListResponse listResponse = PatentListResponse.from(patent);

        assertThat(detailResponse.id()).isEqualTo(1L);
        assertThat(detailResponse.relatedProducts()).containsExactly("Product");
        assertThat(detailResponse.keywords()).containsExactly("Keyword");
        assertThat(detailResponse.initialDepartment()).isEqualTo("Legal");
        assertThat(detailResponse.currentDepartmentId()).isEqualTo(10L);
        assertThat(detailResponse.currentDepartmentName()).isEqualTo("Legal");
        assertThat(detailResponse.createdAt()).isEqualTo(createdAt);
        assertThat(listResponse.title()).isEqualTo("Patent");
        assertThat(listResponse.applicationNumber()).isEqualTo("APP-1");
        assertThat(listResponse.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void changeCurrentDepartmentUpdatesCurrentDepartment() {
        Department before = Department.builder().name("Before").build();
        Department after = Department.builder().name("After").build();
        Patent patent = Patent.builder().title("Patent").applicationNumber("APP-1").currentDepartment(before).build();

        patent.changeCurrentDepartment(after);

        assertThat(patent.getCurrentDepartment()).isSameAs(after);
    }
}
