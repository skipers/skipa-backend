package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.PatentDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatentDepartmentRepository extends JpaRepository<PatentDepartment, Long> { // 특허 담당부서(patent_departments) 저장소 - 배정 이력 조회용

    List<PatentDepartment> findByPatentIdOrderByAssignedAtAsc(Long patentId); // 배정 이력 전체(오래된 순)

    Optional<PatentDepartment> findFirstByPatentIdOrderByAssignedAtAsc(Long patentId); // 최초 배정 부서(가장 오래된 1건)

    Optional<PatentDepartment> findFirstByPatentIdOrderByAssignedAtDesc(Long patentId); // 현재 담당 부서(가장 최신 1건)
}
