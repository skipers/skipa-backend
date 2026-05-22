/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: Department 엔티티에 대한 DB 접근(JPA Repository)을 제공한다.
 * 역할: Service 계층에서 부서(사업부) CRUD를 수행할 수 있도록 기본 영속성 메서드를 제공한다.
 */
package com.skipers.skipa.domain.department.dao;

import com.skipers.skipa.domain.department.domain.Department;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 부서(사업부) 저장소.
 *
 * Spring Data JPA가 기본 CRUD 메서드를 자동으로 제공한다.
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /** 동일한 부서명이 이미 존재하는지 확인한다(중복 방지용). */
    boolean existsByName(String name);

    /** 동일한 부서명이 이미 존재하는지 확인한다(대소문자 무시). */
    boolean existsByNameIgnoreCase(String name);

    /** 부서명을 기준으로 단건 조회한다. */
    Optional<Department> findByName(String name);

    /** 부서명을 기준으로 단건 조회한다(대소문자 무시). */
    Optional<Department> findByNameIgnoreCase(String name);

    /** 부서명 키워드로 목록을 조회한다(페이징 없이 전체 반환). */
    List<Department> findByNameContainingIgnoreCase(String keyword);

    /**
     * 부서명 키워드로 목록을 조회한다(페이지 단위 조회).
     *
     * page/size 기반 API를 만들 때 {@link Pageable}을 그대로 사용할 수 있다.
     */
    Page<Department> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
