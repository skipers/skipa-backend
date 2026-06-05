package com.skipers.skipa.domain.user.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.domain.user.domain.UserStatus;
import com.skipers.skipa.domain.user.dto.request.UserApproveRequest;
import com.skipers.skipa.domain.user.dto.response.UserResponse;
import com.skipers.skipa.domain.user.exception.UserException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public UserResponse approve(Long userId, UserApproveRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new UserException(ErrorCode.CONFLICT);
        }

        Department department = null;
        if (user.getRole() == UserRole.BUSINESS) {
            if (request.departmentId() == null) {
                throw new UserException(ErrorCode.INVALID_REQUEST);
            }

            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new UserException(ErrorCode.DEPARTMENT_NOT_FOUND));
            if (department.isInactive()) {
                throw new UserException(ErrorCode.DEPARTMENT_INACTIVE);
            }
        }

        user.approve(department);
        return UserResponse.from(user);
    }
}
