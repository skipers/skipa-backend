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

@Entity // 연차료 납부 이력(annuity_history) 엔티티
@Getter // 조회 전용(getter) 제공
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
@Table(name = "annuity_history") // 연차료 납부 이력 테이블
public class AnnuityHistory extends BaseTimeEntity { // createdAt/updatedAt 자동 관리

    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto increment
    @Column(name = "id") // 납부 이력 ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 특허(N) : (1) 납부 이력
    @JoinColumn(name = "patent_id", nullable = false) // 특허 FK
    private Patent patent;

    @Column(name = "annuity_year", nullable = false) // 연차(ERD: INT)
    private Integer annuityYear;

    @Column(name = "due_date", nullable = false) // 납부 기한
    private LocalDate dueDate;

    @Column(name = "paid_date") // 실제 납부일자(선택)
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING) // enum → 문자열 저장
    @Column(name = "status", nullable = false, length = 20) // 납부 상태
    private AnnuityStatus status;

    @Column(name = "amount") // 납부 금액(선택)
    private Integer amount;

    @Builder // 생성 시 필요한 값만 선택적으로 세팅
    private AnnuityHistory(
            Patent patent,
            Integer annuityYear,
            LocalDate dueDate,
            LocalDate paidDate,
            AnnuityStatus status,
            Integer amount
    ) {
        this.patent = patent;
        this.annuityYear = annuityYear;
        this.dueDate = dueDate;
        this.paidDate = paidDate;
        this.status = status;
        this.amount = amount;
    }
}

