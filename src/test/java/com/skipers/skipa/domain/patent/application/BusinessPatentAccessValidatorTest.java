package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessPatentAccessValidatorTest {

    @Mock
    private PatentRepository patentRepository;

    private BusinessPatentAccessValidator validator;
    private Department department;

    @BeforeEach
    void setUp() {
        validator = new BusinessPatentAccessValidator(patentRepository);
        department = department(1L, "통신");
    }

    @Test
    void validateAllowsBusinessUserAssignedToPatentDepartment() {
        User user = user(UserRole.BUSINESS, department);
        Patent patent = patent(department);
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));

        validator.validate(user, 10L);
    }

    @Test
    void validateRejectsBusinessUserAssignedToAnotherDepartment() {
        User user = user(UserRole.BUSINESS, department);
        Patent patent = patent(department(2L, "제조"));
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));

        assertPatentError(() -> validator.validate(user, 10L), ErrorCode.FORBIDDEN);
    }

    @Test
    void validateRejectsMissingPatentForBusinessUser() {
        User user = user(UserRole.BUSINESS, department);
        when(patentRepository.findById(10L)).thenReturn(Optional.empty());

        assertPatentError(() -> validator.validate(user, 10L), ErrorCode.PATENT_NOT_FOUND);
    }

    @Test
    void validateSkipsDepartmentCheckForLegalUser() {
        validator.validate(user(UserRole.LEGAL, null), 10L);

        verify(patentRepository, never()).findById(10L);
    }

    private Patent patent(Department currentDepartment) {
        Patent patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .currentDepartment(currentDepartment)
                .build();
        ReflectionTestUtils.setField(patent, "id", 10L);
        return patent;
    }

    private User user(UserRole role, Department department) {
        return User.createActive(
                role.name().toLowerCase(),
                role.name(),
                role.name().toLowerCase() + "@example.com",
                "password",
                role,
                department
        );
    }

    private Department department(Long id, String name) {
        Department department = Department.builder().name(name).build();
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    private void assertPatentError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(PatentException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
