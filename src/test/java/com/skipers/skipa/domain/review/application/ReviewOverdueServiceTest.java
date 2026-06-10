package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewOverdueServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewOverdueService reviewOverdueService;

    @Test
    void markOverdueReviewsUpdatesScheduledAndPendingReviewsPastDueDate() {
        when(reviewRepository.markOverdueByDueDateBefore(
                LocalDate.now(),
                List.of(ReviewStatus.SCHEDULED, ReviewStatus.PENDING)
        )).thenReturn(3);

        int updated = reviewOverdueService.markOverdueReviews();

        assertThat(updated).isEqualTo(3);
        verify(reviewRepository).markOverdueByDueDateBefore(
                LocalDate.now(),
                List.of(ReviewStatus.SCHEDULED, ReviewStatus.PENDING)
        );
    }
}
