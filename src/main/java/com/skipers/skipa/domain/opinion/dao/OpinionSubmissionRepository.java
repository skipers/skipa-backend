package com.skipers.skipa.domain.opinion.dao;

import com.skipers.skipa.domain.opinion.domain.OpinionSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpinionSubmissionRepository extends JpaRepository<OpinionSubmission, Long> {
}
