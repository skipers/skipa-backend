package com.skipers.skipa.domain.user.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.domain.user.domain.UserStatus;
import com.skipers.skipa.domain.user.dto.request.UserApproveRequest;
import com.skipers.skipa.domain.user.dto.response.UserResponse;
import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User pendingUser;
    private Department department;

    @BeforeEach
    void setUp() {
        pendingUser = User.builder()
                .loginId("pending")
                .name("Pending User")
                .email("pending@example.com")
                .password("encoded-password")
                .role(UserRole.BUSINESS)
                .build();
        ReflectionTestUtils.setField(pendingUser, "id", 1L);

        department = Department.builder().name("Semiconductor").build();
        ReflectionTestUtils.setField(department, "id", 10L);
    }

    @Test
    void approveActivatesPendingUserAndAssignsDepartment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(pendingUser));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));

        UserResponse response = adminUserService.approve(1L, new UserApproveRequest(10L));

        assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(pendingUser.getDepartment()).isSameAs(department);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.departmentId()).isEqualTo(10L);
    }

    @Test
    void approveRejectsUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> adminUserService.approve(99L, new UserApproveRequest(10L)), ErrorCode.USER_NOT_FOUND);

        verify(departmentRepository, never()).findById(10L);
    }

    @Test
    void approveRejectsAlreadyActiveUser() {
        User activeUser = User.createActive(
                "active",
                "Active User",
                "active@example.com",
                "encoded-password",
                UserRole.BUSINESS,
                department
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertErrorCode(() -> adminUserService.approve(1L, new UserApproveRequest(10L)), ErrorCode.CONFLICT);

        verify(departmentRepository, never()).findById(10L);
    }

    @Test
    void approveRejectsUnknownDepartment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(pendingUser));
        when(departmentRepository.findById(10L)).thenReturn(Optional.empty());

        assertErrorCode(
                () -> adminUserService.approve(1L, new UserApproveRequest(10L)),
                ErrorCode.DEPARTMENT_NOT_FOUND
        );

        assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.PENDING);
    }

    private void assertErrorCode(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
