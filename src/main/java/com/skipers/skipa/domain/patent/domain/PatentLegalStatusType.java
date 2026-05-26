package com.skipers.skipa.domain.patent.domain;

public enum PatentLegalStatusType { // 특허 권리 상태(ERD: 공개/등록/거절/포기/소멸/무효/취하)
    DISCLOSED, // 공개
    REGISTERED, // 등록
    REJECTED, // 거절
    ABANDONED, // 포기
    EXPIRED, // 소멸
    INVALIDATED, // 무효
    WITHDRAWN // 취하
}

