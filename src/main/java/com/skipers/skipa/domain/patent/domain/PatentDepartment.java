package com.skipers.skipa.domain.patent.domain;

import com.skipers.skipa.domain.department.domain.Department;
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

import java.time.Instant;

@Entity // 특허 담당 부서(patent_departments) 엔티티
@Getter // 조회 전용(getter) 제공
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
@Table(name = "patent_departments") // 특허-부서 매핑 테이블
public class PatentDepartment extends BaseTimeEntity { // createdAt/updatedAt 자동 관리

    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto increment
    @Column(name = "id") // 매핑 ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 특허(N) : (1) 매핑
    @JoinColumn(name = "patent_id", nullable = false) // 특허 FK
    private Patent patent;

    @ManyToOne(fetch = FetchType.LAZY) // 부서(N) : (1) 매핑
    @JoinColumn(name = "department_id", nullable = false) // 부서 FK
    private Department department;

    @Column(name = "assigned_at", nullable = false) // 배정일시
    private Instant assignedAt;

    @Builder // 생성 시 필요한 값만 선택적으로 세팅
    private PatentDepartment(Patent patent, Department department, Instant assignedAt) {
        this.patent = patent;
        this.department = department;
        this.assignedAt = assignedAt;
    }
}

