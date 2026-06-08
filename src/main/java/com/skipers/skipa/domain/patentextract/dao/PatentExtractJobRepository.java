package com.skipers.skipa.domain.patentextract.dao;

import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatentExtractJobRepository extends JpaRepository<PatentExtractJob, Long> {
}
