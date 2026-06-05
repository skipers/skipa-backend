package com.skipers.skipa.domain.user.domain;

import com.skipers.skipa.domain.auth.dto.response.LoginResponse;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.user.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void newUserDefaultsToPendingAndActiveFactoryCreatesActiveUser() {
        User pendingUser = user("pending", UserRole.LEGAL);
        User activeUser = User.createActive(
                "active",
                "Active",
                "active@example.com",
                "password",
                UserRole.BUSINESS,
                null
        );

        assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void approveUpdateAndPasswordUpdateChangeUserState() {
        Department department = department(10L, "Manufacturing");
        User user = user("pending", UserRole.LEGAL);

        user.approve(department);
        user.update("Changed", "changed@example.com", UserRole.BUSINESS, department);
        user.updatePassword("new-password");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getDepartment()).isSameAs(department);
        assertThat(user.getName()).isEqualTo("Changed");
        assertThat(user.getEmail()).isEqualTo("changed@example.com");
        assertThat(user.getRole()).isEqualTo(UserRole.BUSINESS);
        assertThat(user.getPassword()).isEqualTo("new-password");
    }

    @Test
    void responseFactoriesExposeDepartmentAndAuthenticationInformation() {
        Department department = department(10L, "Manufacturing");
        User user = User.createActive(
                "business",
                "Business",
                "business@example.com",
                "password",
                UserRole.BUSINESS,
                department
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        UserResponse userResponse = UserResponse.from(user);
        LoginResponse loginResponse = LoginResponse.of("access", "refresh", user);

        assertThat(userResponse.departmentId()).isEqualTo(10L);
        assertThat(userResponse.departmentName()).isEqualTo("Manufacturing");
        assertThat(userResponse.status()).isEqualTo("ACTIVE");
        assertThat(loginResponse.accessToken()).isEqualTo("access");
        assertThat(loginResponse.refreshToken()).isEqualTo("refresh");
        assertThat(loginResponse.user().loginId()).isEqualTo("business");
        assertThat(loginResponse.user().departmentId()).isEqualTo(10L);
    }

    @Test
    void roleParsingIsCaseInsensitiveAndRejectsUnsupportedValues() {
        assertThat(UserRole.from("legal")).isEqualTo(UserRole.LEGAL);
        assertThatThrownBy(() -> UserRole.from("manager"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private User user(String loginId, UserRole role) {
        return User.builder()
                .loginId(loginId)
                .name("User")
                .email(loginId + "@example.com")
                .password("password")
                .role(role)
                .build();
    }

    private Department department(Long id, String name) {
        Department department = Department.builder().name(name).build();
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }
}
