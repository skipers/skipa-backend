package com.skipers.skipa.domain.department.domain;

import com.skipers.skipa.domain.department.dto.response.DepartmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentTest {

    @Test
    void updateNameChangesDepartmentName() {
        Department department = Department.builder().name("Old").build();

        department.updateName("New");

        assertThat(department.getName()).isEqualTo("New");
    }

    @Test
    void responseFactoryMapsDepartmentFields() {
        Department department = Department.builder().name("Telecom").build();
        Instant createdAt = Instant.parse("2026-05-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-05-02T00:00:00Z");
        ReflectionTestUtils.setField(department, "id", 1L);
        ReflectionTestUtils.setField(department, "createdAt", createdAt);
        ReflectionTestUtils.setField(department, "updatedAt", updatedAt);

        DepartmentResponse response = DepartmentResponse.from(department);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Telecom");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }
}
