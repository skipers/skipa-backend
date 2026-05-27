package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.PatentDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatentDepartmentRepository extends JpaRepository<PatentDepartment, Long> {

    void deleteAllByPatentId(Long patentId);
}
