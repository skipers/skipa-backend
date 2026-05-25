/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 목록 API의 페이지 응답 형태를 고정 포맷으로 제공한다.
 * 역할: 프론트가 `items/page/size/total...`만 보고 일관되게 처리할 수 있도록 한다.
 *
 * 사용법:
 * - `PageResponse.from(page)`로 스프링 `Page`를 변환한다.
 */
package com.skipers.skipa.global.response;

import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * 페이지 응답 DTO.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageResponse<T> {

    /** 현재 페이지의 아이템 목록. */
    private List<T> items;

    /** 현재 페이지 번호(0부터 시작). */
    private int page;

    /** 페이지 크기. */
    private int size;

    /** 전체 아이템 수. */
    private long totalItems;

    /** 전체 페이지 수. */
    private int totalPages;

    /** 다음 페이지 존재 여부. */
    private boolean hasNext;

    /** 이전 페이지 존재 여부. */
    private boolean hasPrevious;

    private PageResponse(
            List<T> items,
            int page,
            int size,
            long totalItems,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    /** 스프링 `Page`를 고정 포맷 페이지 응답으로 변환한다. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}

