package com.skipers.skipa.domain.review.domain;

import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "review_cycles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_cycles_name", columnNames = "name")
        }
)
public class ReviewCycle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 검토 주기 ID
    private Long id;

    @Column(name = "name", nullable = false, length = 100) // 검토 주기명
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20) // 검토 주기 유형
    private ReviewCycleType type;

    @Column(name = "start_date", nullable = false) // 시작일
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false) // 종료일
    private LocalDate endDate;

    @Builder
    private ReviewCycle(String name, ReviewCycleType type, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
