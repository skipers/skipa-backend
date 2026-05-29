package com.skipers.skipa.domain.patent.domain;

import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "annuity_history")
public class AnnuityHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 연차료 납부 이력 ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 특허(N) : (1) 매핑
    @JoinColumn(name = "patent_id", nullable = false)
    private Patent patent;

    @Column(name = "annuity_year", nullable = false) // 연차
    private Integer annuityYear;

    @Column(name = "due_date") // 납부 기한
    private LocalDate dueDate;

    @Column(name = "paid_date") // 실제 납부일자
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20) // 납부 상태(납부/미납/포기)
    private AnnuityStatus status;

    @Column(name = "amount") // 납부 금액
    private Integer amount;

    @Builder
    private AnnuityHistory(Patent patent, Integer annuityYear, LocalDate dueDate, LocalDate paidDate, AnnuityStatus status, Integer amount) {
        this.patent = patent;
        this.annuityYear = annuityYear;
        this.dueDate = dueDate;
        this.paidDate = paidDate;
        this.status = status;
        this.amount = amount;
    }
}
