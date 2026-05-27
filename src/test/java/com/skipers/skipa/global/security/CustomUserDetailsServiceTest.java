package com.skipers.skipa.global.security;

import com.skipers.skipa.domain.auth.exception.AuthException;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserReturnsDetailsForActiveUser() {
        User activeUser = User.createActive(
                "legal",
                "Legal User",
                "legal@example.com",
                "encoded-password",
                UserRole.LEGAL,
                null
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        CustomUserDetails result = (CustomUserDetails) customUserDetailsService.loadUserByUsername("1");

        assertThat(result.getUser()).isSameAs(activeUser);
        assertThat(result.getRole()).isEqualTo(UserRole.LEGAL);
    }

    @Test
    void loadUserRejectsPendingUser() {
        User pendingUser = User.builder()
                .loginId("pending")
                .name("Pending User")
                .email("pending@example.com")
                .password("encoded-password")
                .role(UserRole.BUSINESS)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(pendingUser));

        assertErrorCode(() -> customUserDetailsService.loadUserByUsername("1"), ErrorCode.PENDING_USER);
    }

    @Test
    void loadUserRejectsMissingUserAsInvalidToken() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertErrorCode(() -> customUserDetailsService.loadUserByUsername("1"), ErrorCode.INVALID_TOKEN);
    }

    private void assertErrorCode(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
