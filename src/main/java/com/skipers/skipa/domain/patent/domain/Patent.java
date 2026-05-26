package com.skipers.skipa.domain.patent.domain;

import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity // 특허 기본 정보(patents) 엔티티
@Getter // 조회 전용(getter) 제공
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
@Table( // 테이블/유니크 제약 정의
        name = "patents",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_patents_application_number", columnNames = "application_number")
        }
)
public class Patent extends BaseTimeEntity { // createdAt/updatedAt 자동 관리

    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto increment
    @Column(name = "id") // 특허 ID
    private Long id;

    @Column(name = "title", length = 500, nullable = false) // 특허명
    private String title;

    @Column(name = "application_number", length = 20, nullable = false) // 출원번호(유니크)
    private String applicationNumber;

    @Column(name = "registration_number", length = 20) // 등록번호
    private String registrationNumber;

    @Column(name = "publication_number", length = 20) // 공개번호
    private String publicationNumber;

    @Column(name = "announcement_number", length = 20) // 공고번호
    private String announcementNumber;

    @Column(name = "application_date") // 출원일자
    private LocalDate applicationDate;

    @Column(name = "registration_date") // 등록일자
    private LocalDate registrationDate;

    @Column(name = "publication_date") // 공개일자
    private LocalDate publicationDate;

    @Column(name = "announcement_date") // 공고일자
    private LocalDate announcementDate;

    @Column(name = "ipc_code", length = 200) // IPC 코드
    private String ipcCode;

    @Column(name = "cpc_code", length = 200) // CPC 코드
    private String cpcCode;

    @Column(name = "applicant", length = 200) // 출원인명
    private String applicant;

    @Column(name = "inventor", length = 500) // 발명자명
    private String inventor;

    @Column(name = "expiry_date") // 예상 소멸일자
    private LocalDate expiryDate;

    @Column(name = "citation_count") // 피인용 수
    private Integer citationCount;

    @Column(name = "original_pdf_key", length = 500) // 원문 파일 키(S3 등)
    private String originalPdfKey;

    @Column(name = "management_number", length = 50) // 관리번호
    private String managementNumber;

    @Column(name = "business_field", length = 200) // 관련사업 분야
    private String businessField;

    @Column(name = "tech_field", length = 200) // 관련기술 분야
    private String techField;

    @Column(name = "related_products") // 관련제품(JSON 문자열) - DB 타입(JSON/TEXT)은 환경별로 정한다
    private String relatedProducts;

    @Column(name = "filing_country", length = 100) // 출원국가
    private String filingCountry;

    @Column(name = "is_joint_application") // 공동출원 여부
    private Boolean isJointApplication;

    @Column(name = "joint_applicant", length = 200) // 공동출원인명
    private String jointApplicant;

    @Column(name = "initial_department", length = 200) // 최초 담당 부서명(초기값 기록용)
    private String initialDepartment;

    @Column(name = "keywords") // 키워드(JSON 문자열) - DB 타입(JSON/TEXT)은 환경별로 정한다
    private String keywords;

    @Column(name = "overview", columnDefinition = "text") // 개요
    private String overview;

    @Column(name = "core_content", columnDefinition = "text") // 핵심 내용
    private String coreContent;

    @Builder // 생성 시 필요한 값만 선택적으로 세팅
    private Patent( // 생성자는 builder로만 사용
            String title,
            String applicationNumber,
            String registrationNumber,
            String publicationNumber,
            String announcementNumber,
            LocalDate applicationDate,
            LocalDate registrationDate,
            LocalDate publicationDate,
            LocalDate announcementDate,
            String ipcCode,
            String cpcCode,
            String applicant,
            String inventor,
            LocalDate expiryDate,
            Integer citationCount,
            String originalPdfKey,
            String managementNumber,
            String businessField,
            String techField,
            String relatedProducts,
            String filingCountry,
            Boolean isJointApplication,
            String jointApplicant,
            String initialDepartment,
            String keywords,
            String overview,
            String coreContent
    ) {
        this.title = title;
        this.applicationNumber = applicationNumber;
        this.registrationNumber = registrationNumber;
        this.publicationNumber = publicationNumber;
        this.announcementNumber = announcementNumber;
        this.applicationDate = applicationDate;
        this.registrationDate = registrationDate;
        this.publicationDate = publicationDate;
        this.announcementDate = announcementDate;
        this.ipcCode = ipcCode;
        this.cpcCode = cpcCode;
        this.applicant = applicant;
        this.inventor = inventor;
        this.expiryDate = expiryDate;
        this.citationCount = citationCount;
        this.originalPdfKey = originalPdfKey;
        this.managementNumber = managementNumber;
        this.businessField = businessField;
        this.techField = techField;
        this.relatedProducts = relatedProducts;
        this.filingCountry = filingCountry;
        this.isJointApplication = isJointApplication;
        this.jointApplicant = jointApplicant;
        this.initialDepartment = initialDepartment;
        this.keywords = keywords;
        this.overview = overview;
        this.coreContent = coreContent;
    }

    public void updateTitle(String title) { // 특허명 변경(PATCH 지원을 위한 최소 변경 메서드)
        this.title = title;
    }

}
