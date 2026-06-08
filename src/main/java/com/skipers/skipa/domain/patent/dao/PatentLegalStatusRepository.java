package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatentLegalStatusRepository extends JpaRepository<PatentLegalStatus, Long> {

    Page<PatentLegalStatus> findByPatentId(Long patentId, Pageable pageable);

    Optional<PatentLegalStatus> findFirstByPatentIdOrderByChangedAtDescIdDesc(Long patentId);

    void deleteAllByPatentId(Long patentId);
}
