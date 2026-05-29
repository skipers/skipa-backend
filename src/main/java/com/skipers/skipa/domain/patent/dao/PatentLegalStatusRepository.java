package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatentLegalStatusRepository extends JpaRepository<PatentLegalStatus, Long> {

    Page<PatentLegalStatus> findByPatentId(Long patentId, Pageable pageable);

    void deleteAllByPatentId(Long patentId);
}

