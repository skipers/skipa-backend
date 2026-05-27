package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.Patent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PatentRepository extends JpaRepository<Patent, Long>, JpaSpecificationExecutor<Patent> { // 특허(Patent) 저장소 + 동적 검색 지원

    boolean existsByApplicationNumber(String applicationNumber); // 출원번호 중복 여부 확인

    Optional<Patent> findByApplicationNumber(String applicationNumber); // 출원번호로 단건 조회
}
