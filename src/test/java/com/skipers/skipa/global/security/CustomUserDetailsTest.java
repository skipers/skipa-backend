package com.skipers.skipa.global.security;

import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void exposesUserIdentityAuthorityAndEnabledState() {
        User user = User.createActive(
                "admin",
                "Admin",
                "admin@example.com",
                "encoded-password",
                UserRole.ADMIN,
                null
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getUser()).isSameAs(user);
        assertThat(details.getId()).isEqualTo(1L);
        assertThat(details.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }
}
