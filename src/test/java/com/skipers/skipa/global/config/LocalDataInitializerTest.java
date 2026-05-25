package com.skipers.skipa.global.config;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private LocalDataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new LocalDataInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "seedPassword", "1234");
    }

    @Test
    void createsTenEncodedSampleUsersWhenLocalDatabaseHasNoUsers() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("1234")).thenReturn("encoded-password");

        initializer.run(new DefaultApplicationArguments());

        verify(userRepository).saveAll(org.mockito.ArgumentMatchers.argThat(users -> {
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
    void doesNotCreateSeedUserWhenLocalDatabaseAlreadyHasUsers() {
        when(userRepository.count()).thenReturn(1L);

        initializer.run(new DefaultApplicationArguments());

        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
        verify(userRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }
}
