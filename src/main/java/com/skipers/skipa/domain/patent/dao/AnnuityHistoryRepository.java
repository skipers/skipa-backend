package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.AnnuityHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnuityHistoryRepository extends JpaRepository<AnnuityHistory, Long> { // 연차료 납부 이력(annuity_history) 저장소 - 추후 확장
}

