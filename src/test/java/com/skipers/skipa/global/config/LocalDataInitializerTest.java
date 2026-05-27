package com.skipers.skipa.global.config;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private LocalDataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new LocalDataInitializer(userRepository, departmentRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "seedPassword", "1234");
    }

    @Test
    void createsDepartmentsAndTenEncodedSampleUsersWhenLocalDatabaseHasNoUsers() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("1234")).thenReturn("encoded-password");

        Department semiconductor = Department.builder().name("반도체").build();
        Department telecom = Department.builder().name("통신").build();
        Department manufacturing = Department.builder().name("제조").build();
        when(departmentRepository.saveAll(anyList())).thenReturn(List.of(semiconductor, telecom, manufacturing));

        initializer.run(new DefaultApplicationArguments());

        verify(departmentRepository).saveAll(argThat(departments -> {
            List<Department> result = new java.util.ArrayList<>();
            departments.forEach(result::add);
            assertThat(result).hasSize(3);
            assertThat(result).extracting(Department::getName)
                    .contains("반도체", "통신", "제조");
            return true;
        }));

        verify(userRepository).saveAll(argThat(users -> {
            List<User> result = new java.util.ArrayList<>();
            users.forEach(result::add);
            assertThat(result).hasSize(10);
            assertThat(result).filteredOn(user -> user.getRole() == UserRole.ADMIN).hasSize(1);
            assertThat(result).filteredOn(user -> user.getRole() == UserRole.LEGAL).hasSize(4);
            assertThat(result).filteredOn(user -> user.getRole() == UserRole.BUSINESS).hasSize(5);
            assertThat(result).allMatch(user -> user.getPassword().equals("encoded-password"));
            assertThat(result).extracting(User::getLoginId)
                    .contains("admin", "legal01", "legal04", "business01", "business05");
            return true;
        }));
    }

    @Test
    void doesNotCreateSeedDataWhenLocalDatabaseAlreadyHasUsers() {
        when(userRepository.count()).thenReturn(1L);

        initializer.run(new DefaultApplicationArguments());

        verify(passwordEncoder, never()).encode(anyString());
        verify(departmentRepository, never()).saveAll(anyList());
        verify(userRepository, never()).saveAll(anyList());
    }
}
