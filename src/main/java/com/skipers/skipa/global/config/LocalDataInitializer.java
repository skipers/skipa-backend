package com.skipers.skipa.global.config;

import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.local.seed.password}")
    private String seedPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        String encodedPassword = passwordEncoder.encode(seedPassword);
        List<User> seedUsers = List.of(
                buildUser("admin", "관리자", "admin@skipa.local", encodedPassword, UserRole.ADMIN),
                buildUser("legal01", "법무 담당자 1", "legal_01@skipa.local", encodedPassword, UserRole.LEGAL),
                buildUser("legal02", "법무 담당자 2", "legal_02@skipa.local", encodedPassword, UserRole.LEGAL),
                buildUser("legal03", "법무 담당자 3", "legal_03@skipa.local", encodedPassword, UserRole.LEGAL),
                buildUser("legal04", "법무 담당자 4", "legal_04@skipa.local", encodedPassword, UserRole.LEGAL),
                buildUser("business01", "사업부 담당자 1", "business_01@skipa.local", encodedPassword, UserRole.BUSINESS),
                buildUser("business02", "사업부 담당자 2", "business_02@skipa.local", encodedPassword, UserRole.BUSINESS),
                buildUser("business03", "사업부 담당자 3", "business_03@skipa.local", encodedPassword, UserRole.BUSINESS),
                buildUser("business04", "사업부 담당자 4", "business_04@skipa.local", encodedPassword, UserRole.BUSINESS),
                buildUser("business05", "사업부 담당자 5", "business_05@skipa.local", encodedPassword, UserRole.BUSINESS)
        );

        userRepository.saveAll(seedUsers);

        log.info("Created {} initial local accounts", seedUsers.size());
    }

    private User buildUser(String loginId, String name, String email, String password, UserRole role) {
        return User.builder()
                .loginId(loginId)
                .name(name)
                .email(email)
                .password(password)
                .role(role)
                .build();
    }
}
