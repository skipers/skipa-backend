package com.skipers.skipa.global.config;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
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
import java.util.Optional;

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

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private PatentLegalStatusRepository patentLegalStatusRepository;

    @Mock
    private PatentAnnuityRepository patentAnnuityRepository;

    @Mock
    private ReviewCycleRepository reviewCycleRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReportRepository reportRepository;

    private LocalDataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new LocalDataInitializer(
                userRepository,
                departmentRepository,
                passwordEncoder,
                patentRepository,
                patentLegalStatusRepository,
                patentAnnuityRepository,
                reviewCycleRepository,
                reviewRepository,
                reportRepository
        );
        ReflectionTestUtils.setField(initializer, "seedPassword", "1234");
    }

    @Test
    void createsDepartmentsAndTenEncodedSampleUsersWhenLocalDatabaseHasNoUsers() {
        when(userRepository.count()).thenReturn(0L);
        when(patentRepository.count()).thenReturn(1L);
        when(passwordEncoder.encode("1234")).thenReturn("encoded-password");

        when(departmentRepository.findByName("반도체")).thenReturn(Optional.empty());
        when(departmentRepository.findByName("통신")).thenReturn(Optional.empty());
        when(departmentRepository.findByName("제조")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer.run(new DefaultApplicationArguments());

        verify(departmentRepository).save(argThat(department -> department.getName().equals("반도체")));
        verify(departmentRepository).save(argThat(department -> department.getName().equals("통신")));
        verify(departmentRepository).save(argThat(department -> department.getName().equals("제조")));

        verify(userRepository).saveAll(argThat(users -> {
            List<User> result = new java.util.ArrayList<>();
            users.forEach(result::add);
            assertThat(result).hasSize(10);
            assertThat(result).filteredOn(user -> user.getRole() == UserRole.ADMIN).hasSize(1);
            assertThat(result).filteredOn(user -> user.getRole() == UserRole.LEGAL).hasSize(4);
            assertThat(result).filteredOn(user -> user.getRole() == UserRole.BUSINESS).hasSize(5);
            assertThat(result).allMatch(user -> user.getPassword().equals("encoded-password"));
            assertThat(result).extracting(User::getLoginId)
                    .contains("admin", "legal01", "legal04", "biz01", "biz05");
            return true;
        }));
    }

    @Test
    void doesNotCreateSeedDataWhenLocalDatabaseAlreadyHasUsersAndPatents() {
        when(userRepository.count()).thenReturn(1L);
        when(patentRepository.count()).thenReturn(1L);
        Department semiconductor = Department.builder().name("반도체").build();
        Department telecom = Department.builder().name("통신").build();
        Department manufacturing = Department.builder().name("제조").build();
        when(departmentRepository.findByName("반도체")).thenReturn(Optional.of(semiconductor));
        when(departmentRepository.findByName("통신")).thenReturn(Optional.of(telecom));
        when(departmentRepository.findByName("제조")).thenReturn(Optional.of(manufacturing));

        initializer.run(new DefaultApplicationArguments());

        verify(passwordEncoder, never()).encode(anyString());
        verify(departmentRepository, never()).save(any());
        verify(userRepository, never()).saveAll(anyList());
        verify(patentRepository, never()).saveAll(anyList());
    }
}
