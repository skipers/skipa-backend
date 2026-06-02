package com.skipers.skipa.domain.department.dao;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.domain.DepartmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    Optional<Department> findByName(String name);

    Optional<Department> findByNameIgnoreCase(String name);

    List<Department> findByNameContainingIgnoreCase(String keyword);

    Page<Department> findByStatus(DepartmentStatus status, Pageable pageable);

    Page<Department> findByStatusAndNameContainingIgnoreCase(DepartmentStatus status, String keyword, Pageable pageable);
}
