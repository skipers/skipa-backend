package com.skipers.skipa.domain.patent.domain;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "patents",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_patents_application_number", columnNames = "application_number")
        }
)
public class Patent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 특허 ID
    private Long id;

    @Column(name = "title", length = 500, nullable = false) // 특허명
    private String title;

    @Column(name = "application_number", length = 20, nullable = false, unique = true) // 출원번호
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ipc_codes") // IPC 코드
    private List<String> ipcCodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cpc_codes") // CPC 코드
    private List<String> cpcCodes;

    @Column(name = "applicant", length = 200) // 출원인명
    private String applicant;

    @Column(name = "inventor", length = 500) // 발명자명
    private String inventor;

    @Column(name = "expiry_date") // 예상 소멸일자
    private LocalDate expiryDate;

    @Column(name = "citation_count") // 피인용 수
    private Integer citationCount;

    @Column(name = "examination_claim_count") // 심사청구항수
    private Integer examinationClaimCount;

    @Column(name = "original_pdf_key", length = 500) // 원문 파일 키
    private String originalPdfKey;

    @Column(name = "management_number", length = 50) // 관리번호
    private String managementNumber;

    @Column(name = "business_field", length = 200) // 관련사업 분야
    private String businessField;

    @Column(name = "tech_field", length = 200) // 관련기술 분야
    private String techField;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_products") // 관련제품
    private List<String> relatedProducts;

    @Column(name = "filing_country", length = 100) // 출원국가
    private String filingCountry;

    @Column(name = "is_joint_application") // 공동출원 여부
    private Boolean isJointApplication;

    @Column(name = "joint_applicant", length = 200) // 공동출원인명
    private String jointApplicant;

    @Column(name = "initial_department", length = 200) // 최초 담당 부서명
    private String initialDepartment;

    @ManyToOne(fetch = FetchType.LAZY) // 현재 담당 부서(선택)
    @JoinColumn(name = "current_department_id")
    private Department currentDepartment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords") // 키워드
    private List<String> keywords;

    @Column(name = "overview", columnDefinition = "text") // 개요
    private String overview;

    @Column(name = "core_content", columnDefinition = "text") // 핵심 내용
    private String coreContent;

    @Builder
    private Patent(
            String title,
            String applicationNumber,
            String registrationNumber,
            String publicationNumber,
            String announcementNumber,
            LocalDate applicationDate,
            LocalDate registrationDate,
            LocalDate publicationDate,
            LocalDate announcementDate,
            List<String> ipcCodes,
            List<String> cpcCodes,
            String applicant,
            String inventor,
            LocalDate expiryDate,
            Integer citationCount,
            Integer examinationClaimCount,
            String originalPdfKey,
            String managementNumber,
            String businessField,
            String techField,
            List<String> relatedProducts,
            String filingCountry,
            Boolean isJointApplication,
            String jointApplicant,
            String initialDepartment,
            Department currentDepartment,
            List<String> keywords,
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
        this.ipcCodes = ipcCodes;
        this.cpcCodes = cpcCodes;
        this.applicant = applicant;
        this.inventor = inventor;
        this.expiryDate = expiryDate;
        this.citationCount = citationCount;
        this.examinationClaimCount = examinationClaimCount;
        this.originalPdfKey = originalPdfKey;
        this.managementNumber = managementNumber;
        this.businessField = businessField;
        this.techField = techField;
        this.relatedProducts = relatedProducts;
        this.filingCountry = filingCountry;
        this.isJointApplication = isJointApplication;
        this.jointApplicant = jointApplicant;
        this.initialDepartment = initialDepartment;
        this.currentDepartment = currentDepartment;
        this.keywords = keywords;
        this.overview = overview;
        this.coreContent = coreContent;
    }

    public void update(
            String title,
            String applicationNumber,
            String registrationNumber,
            String publicationNumber,
            String announcementNumber,
            LocalDate applicationDate,
            LocalDate registrationDate,
            LocalDate publicationDate,
            LocalDate announcementDate,
            List<String> ipcCodes,
            List<String> cpcCodes,
            String applicant,
            String inventor,
            LocalDate expiryDate,
            Integer citationCount,
            Integer examinationClaimCount,
            String originalPdfKey,
            String managementNumber,
            String businessField,
            String techField,
            List<String> relatedProducts,
            String filingCountry,
            Boolean isJointApplication,
            String jointApplicant,
            String initialDepartment,
            List<String> keywords,
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
        this.ipcCodes = ipcCodes;
        this.cpcCodes = cpcCodes;
        this.applicant = applicant;
        this.inventor = inventor;
        this.expiryDate = expiryDate;
        this.citationCount = citationCount;
        this.examinationClaimCount = examinationClaimCount;
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

    public void changeCurrentDepartment(Department department) {
        this.currentDepartment = department;
    }

}
