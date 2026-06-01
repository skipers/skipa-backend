package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessPatentAccessValidator {

    private final PatentRepository patentRepository;

    public void validate(User user, Long patentId) {
        if (user.getRole() != UserRole.BUSINESS) {
            return;
        }

        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        if (user.getDepartment() == null
                || patent.getCurrentDepartment() == null
                || !patent.getCurrentDepartment().getId().equals(user.getDepartment().getId())) {
            throw new PatentException(ErrorCode.FORBIDDEN);
        }
    }
}
