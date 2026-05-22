/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 사업부(부서) 정보를 저장하는 departments 테이블 엔티티를 정의한다.
 * 역할: 특허 담당부서 배정, 사용자 소속, 사업부 결정 등에서 참조되는 부서 마스터를 제공한다.
 */
package com.skipers.skipa.domain.department.domain;

import com.skipers.skipa.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사업부(부서) 마스터 엔티티.
 *
 * createdAt/updatedAt은 {@link BaseEntity}를 상속받아 자동 관리한다.
 */
@Getter
@Entity
@Table(name = "departments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department extends BaseEntity {

    /** 부서 ID(PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 부서명. */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 부서 생성.
     *
     * @param name 부서명
     */
    public Department(String name) {
        this.name = name;
    }

    /**
     * 부서명을 변경한다.
     *
     * @param name 변경할 부서명
     */
    public void changeName(String name) {
        this.name = name;
    }
}
