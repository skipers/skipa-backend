package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.Patent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatentRepository extends JpaRepository<Patent, Long> { // 특허(Patent) 저장소

    boolean existsByApplicationNumber(String applicationNumber); // 출원번호 중복 여부 확인

    Optional<Patent> findByApplicationNumber(String applicationNumber); // 출원번호로 단건 조회

    Page<Patent> findByTitleContainingIgnoreCase(String keyword, Pageable pageable); // 특허명 키워드로 목록 조회(페이지)
}

