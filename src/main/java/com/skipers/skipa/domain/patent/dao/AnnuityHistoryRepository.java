package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.AnnuityHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnuityHistoryRepository extends JpaRepository<AnnuityHistory, Long> {

    Page<AnnuityHistory> findByPatentId(Long patentId, Pageable pageable);

    void deleteAllByPatentId(Long patentId);
}
