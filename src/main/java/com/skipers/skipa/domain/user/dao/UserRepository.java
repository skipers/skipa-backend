package com.skipers.skipa.domain.user.dao;

import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    @Query("select user from User user left join fetch user.department where user.id = :id")
    Optional<User> findByIdWithDepartment(@Param("id") Long id);

    Optional<User> findByEmail(String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    List<User> findAllByRole(UserRole role);

    List<User> findAllByDepartmentId(Long departmentId);
}
