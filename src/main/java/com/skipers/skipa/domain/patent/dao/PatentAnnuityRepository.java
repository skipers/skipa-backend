package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatentAnnuityRepository extends JpaRepository<PatentAnnuity, Long> {

    Page<PatentAnnuity> findByPatentId(Long patentId, Pageable pageable);

    void deleteAllByPatentId(Long patentId);
}
