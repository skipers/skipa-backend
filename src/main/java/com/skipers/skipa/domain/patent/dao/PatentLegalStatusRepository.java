package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatentLegalStatusRepository extends JpaRepository<PatentLegalStatus, Long> { // 특허 권리상태 이력(patent_legal_status) 저장소 - 추후 확장
}

