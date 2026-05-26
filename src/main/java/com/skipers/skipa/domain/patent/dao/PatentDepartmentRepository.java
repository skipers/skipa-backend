package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.PatentDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatentDepartmentRepository extends JpaRepository<PatentDepartment, Long> { // 특허 담당부서(patent_departments) 저장소 - 추후 확장
}

