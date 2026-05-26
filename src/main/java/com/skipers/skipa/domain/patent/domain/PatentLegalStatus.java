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

@Entity // 권리 상태 이력(patent_legal_status) 엔티티
@Getter // 조회 전용(getter) 제공
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
@Table(name = "patent_legal_status") // 권리 상태 이력 테이블
public class PatentLegalStatus extends BaseTimeEntity { // createdAt/updatedAt 자동 관리

    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto increment
    @Column(name = "id") // 이력 ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 특허(N) : (1) 이력
    @JoinColumn(name = "patent_id", nullable = false) // 특허 FK
    private Patent patent;

    @Enumerated(EnumType.STRING) // enum → 문자열 저장
    @Column(name = "status", nullable = false, length = 50) // 권리 상태
    private PatentLegalStatusType status;

    @Column(name = "changed_at", nullable = false) // 상태 변경일자(ERD: DATE)
    private LocalDate changedAt;

    @Builder // 생성 시 필요한 값만 선택적으로 세팅
    private PatentLegalStatus(Patent patent, PatentLegalStatusType status, LocalDate changedAt) {
        this.patent = patent;
        this.status = status;
        this.changedAt = changedAt;
    }
}

