package com.skipers.skipa.domain.user.domain;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "login_id", length = 50, nullable = false, unique = true)
    private String loginId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "email", length = 200, nullable = false, unique = true)
    private String email;

    @Column(name = "password", length = 200, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Builder
    private User(
            String loginId,
            String name,
            String email,
            String password,
            UserRole role,
            Department department,
            UserStatus status
    ) {
        this.loginId = loginId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.department = department;
        this.status = status != null ? status : UserStatus.PENDING;
    }

    public static User createActive(
            String loginId,
            String name,
            String email,
            String password,
            UserRole role,
            Department department
    ) {
        return User.builder()
                .loginId(loginId)
                .name(name)
                .email(email)
                .password(password)
                .role(role)
                .department(department)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public void approve(Department department) {
        this.status = UserStatus.ACTIVE;
        this.department = department;
    }

    public void update(String name, String email, UserRole role, Department department) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.department = department;
    }

    public void updatePassword(String password) {
        this.password = password;
    }
}
