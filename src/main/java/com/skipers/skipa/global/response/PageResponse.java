package com.skipers.skipa.global.response;

import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageResponse<T> {

    private List<T> items; // 현재 페이지의 아이템 목록

    private int page; // 현재 페이지 번호(0부터 시작)

    private int size; // 페이지 크기

    private long totalItems; // 전체 아이템 수

    private int totalPages; // 전체 페이지 수

    private boolean hasNext; // 다음 페이지 존재 여부

    private boolean hasPrevious; // 이전 페이지 존재 여부

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

