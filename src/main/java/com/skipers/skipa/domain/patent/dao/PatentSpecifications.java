package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.Patent;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class PatentSpecifications { // 특허 검색 조건(Specification) 모음 - 조건 추가/삭제 시 여기만 확장

    private PatentSpecifications() { // 유틸리티 클래스(인스턴스화 방지)
    }

    public static Specification<Patent> titleContainsIgnoreCase(String keyword) { // v1: 제목 부분검색(대소문자 무시)
        if (keyword == null) {
            return (root, query, cb) -> cb.conjunction(); // 검색어 없으면 전체 조회
        }

        String likeKeyword = "%" + keyword.toLowerCase(Locale.ROOT) + "%"; // LIKE 패턴 생성
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), likeKeyword); // title like %keyword%
    }
}

