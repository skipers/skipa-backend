package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewOverdueService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public int markOverdueReviews() {
        return reviewRepository.markOverdueByDueDateBefore(
                LocalDate.now(),
                List.of(ReviewStatus.SCHEDULED, ReviewStatus.PENDING)
        );
    }
}
