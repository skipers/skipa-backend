package com.skipers.skipa.domain.review.dao;

import com.skipers.skipa.domain.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByDepartmentId(Long departmentId, Pageable pageable);

    Optional<Review> findByPatentIdAndDepartmentId(Long patentId, Long departmentId);

    boolean existsByPatentIdAndDepartmentId(Long patentId, Long departmentId);

    boolean existsByPatentId(Long patentId);

    void deleteAllByPatentId(Long patentId);
}
