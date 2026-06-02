package com.skipers.skipa.domain.review.dao;

import com.skipers.skipa.domain.review.domain.ReviewCycle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, Long> {

    Optional<ReviewCycle> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            LocalDate startDate,
            LocalDate endDate
    );

    Page<ReviewCycle> findAllByOrderByStartDateDesc(Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long reviewCycleId);

    boolean existsByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate endDate, LocalDate startDate);

    boolean existsByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
            LocalDate endDate,
            LocalDate startDate,
            Long reviewCycleId
    );
}
