package com.skipers.skipa.domain.department.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.dto.request.DepartmentCreateRequest;
import com.skipers.skipa.domain.department.dto.request.DepartmentUpdateRequest;
import com.skipers.skipa.domain.department.dto.response.DepartmentResponse;
import com.skipers.skipa.domain.department.exception.DepartmentException;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder().name("Telecom").build();
        ReflectionTestUtils.setField(department, "id", 1L);
    }

    @Test
    void createPreservesAndSavesNewDepartmentName() {
        when(departmentRepository.save(org.mockito.ArgumentMatchers.any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentResponse response = departmentService.create(new DepartmentCreateRequest("  Telecom  "));

        verify(departmentRepository).existsByNameIgnoreCase("  Telecom  ");
        verify(departmentRepository).save(org.mockito.ArgumentMatchers.argThat(saved -> saved.getName().equals("  Telecom  ")));
        assertThat(response.name()).isEqualTo("  Telecom  ");
    }

    @Test
    void createRejectsDuplicateNameIgnoringCase() {
        when(departmentRepository.existsByNameIgnoreCase(" Telecom ")).thenReturn(true);

        assertDepartmentError(
                () -> departmentService.create(new DepartmentCreateRequest(" Telecom ")),
                ErrorCode.DUPLICATE_DEPARTMENT_NAME
        );

        verify(departmentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getReturnsDepartmentAndRejectsMissingId() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(departmentService.get(1L).name()).isEqualTo("Telecom");
        assertDepartmentError(() -> departmentService.get(2L), ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    void getAllWithoutKeywordUsesPagedFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Pageable sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        when(departmentRepository.findAll(sortedPageable)).thenReturn(new PageImpl<>(List.of(department), sortedPageable, 1));

        Page<DepartmentResponse> result = departmentService.getAll("  ", pageable);

        assertThat(result.getContent()).extracting(DepartmentResponse::name).containsExactly("Telecom");
        verify(departmentRepository).findAll(sortedPageable);
        verify(departmentRepository, never()).findByNameContainingIgnoreCase(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getAllWithKeywordNormalizesAndSearchesByName() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        Pageable sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        when(departmentRepository.findByNameContainingIgnoreCase("tele", sortedPageable))
                .thenReturn(new PageImpl<>(List.of(department), sortedPageable, 1));

        Page<DepartmentResponse> result = departmentService.getAll(" tele ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(departmentRepository).findByNameContainingIgnoreCase("tele", sortedPageable);
    }

    @Test
    void updatePreservesChangedName() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        DepartmentResponse response = departmentService.update(1L, new DepartmentUpdateRequest(" New Telecom "));

        assertThat(response.name()).isEqualTo(" New Telecom ");
        assertThat(department.getName()).isEqualTo(" New Telecom ");
        verify(departmentRepository).existsByNameIgnoreCase(" New Telecom ");
    }

    @Test
    void updateRejectsDuplicateChangedName() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByNameIgnoreCase("Manufacturing")).thenReturn(true);

        assertDepartmentError(
                () -> departmentService.update(1L, new DepartmentUpdateRequest("Manufacturing")),
                ErrorCode.DUPLICATE_DEPARTMENT_NAME
        );

        assertThat(department.getName()).isEqualTo("Telecom");
    }

    @Test
    void updateWithSameNameIgnoringCaseDoesNotCheckDuplicate() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        departmentService.update(1L, new DepartmentUpdateRequest("telecom"));

        verify(departmentRepository, never()).existsByNameIgnoreCase(org.mockito.ArgumentMatchers.any());
        assertThat(department.getName()).isEqualTo("telecom");
    }

    @Test
    void updateRejectsMissingDepartment() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertDepartmentError(
                () -> departmentService.update(99L, new DepartmentUpdateRequest("Other")),
                ErrorCode.DEPARTMENT_NOT_FOUND
        );
    }

    @Test
    void deleteRemovesExistingDepartmentAndRejectsMissingId() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(departmentRepository.existsById(2L)).thenReturn(false);

        departmentService.delete(1L);

        verify(userRepository).existsByDepartmentId(1L);
        verify(departmentRepository).deleteById(1L);
        assertDepartmentError(() -> departmentService.delete(2L), ErrorCode.DEPARTMENT_NOT_FOUND);
        verify(userRepository, never()).existsByDepartmentId(2L);
        verify(departmentRepository, never()).deleteById(2L);
    }

    @Test
    void deleteRejectsDepartmentAssignedToUser() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertDepartmentError(() -> departmentService.delete(1L), ErrorCode.DEPARTMENT_IN_USE);

        verify(departmentRepository, never()).deleteById(1L);
    }

    private void assertDepartmentError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(DepartmentException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
