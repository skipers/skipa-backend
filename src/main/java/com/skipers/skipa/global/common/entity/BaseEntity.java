/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 엔티티의 '생성일시/수정일시' 컬럼을 공통으로 제공한다.
 * 역할: 모든 JPA 엔티티가 상속해서 created_at/updated_at을 자동으로 가지게 한다.
 *
 * 사용법:
 * - 엔티티 클래스가 `BaseEntity`를 `extends` 하면 됨.
 * - 저장 시 `createdAt/updatedAt`이 UTC 기준으로 자동 세팅되고, 수정 시 `updatedAt`이 UTC 기준으로 자동 갱신됨.
 */
package com.skipers.skipa.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Clock;
import java.time.Instant;
import lombok.Getter;

/**
 * 공통 생성/수정 일시를 제공하는 베이스 엔티티.
 *
 * 저장/수정 시각은 UTC 기준으로 기록한다.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

    /** 애플리케이션 전역 기준 시간(UTC). */
    private static final Clock CLOCK = Clock.systemUTC();

    /** 엔티티 생성일시(최초 저장 시점). */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 엔티티 수정일시(마지막 변경 시점). */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** 엔티티가 최초 저장되기 직전에 생성/수정 일시를 초기화합니다. */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now(CLOCK);
        createdAt = now;
        updatedAt = now;
    }

    /** 엔티티가 업데이트되기 직전에 수정 일시를 갱신합니다. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now(CLOCK);
    }
}
