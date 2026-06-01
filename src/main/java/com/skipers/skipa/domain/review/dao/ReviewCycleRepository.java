package com.skipers.skipa.domain.review.dao;

import com.skipers.skipa.domain.review.domain.ReviewCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, Long> {

    Optional<ReviewCycle> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            LocalDate startDate,
            LocalDate endDate
    );
}
