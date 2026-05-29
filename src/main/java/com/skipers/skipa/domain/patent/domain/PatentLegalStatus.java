package com.skipers.skipa.domain.patent.domain;

import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "patent_legal_status")
public class PatentLegalStatus extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 권리 상태 이력 ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 특허(N) : (1) 매핑
    @JoinColumn(name = "patent_id", nullable = false)
    private Patent patent;

    @Column(name = "status", nullable = false) // 권리 상태
    private String status;

    @Column(name = "changed_at") // 상태 변경일자
    private LocalDate changedAt;

    @Builder
    private PatentLegalStatus(Patent patent, String status, LocalDate changedAt) {
        this.patent = patent;
        this.status = status;
        this.changedAt = changedAt;
    }
}

