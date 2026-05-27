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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "patent_departments")
public class PatentDepartment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 매핑 ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 특허(N) : (1) 매핑
    @JoinColumn(name = "patent_id", nullable = false)
    private Patent patent;

    @ManyToOne(fetch = FetchType.LAZY) // 부서(N) : (1) 매핑
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "assigned_at", nullable = false) // 배정일시
    private Instant assignedAt;

    @Builder
    private PatentDepartment(Patent patent, Department department, Instant assignedAt) {
        this.patent = patent;
        this.department = department;
        this.assignedAt = assignedAt;
    }
}
