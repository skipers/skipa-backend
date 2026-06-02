package com.skipers.skipa.domain.patent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.request.PatentCreateRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentUpdateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentDetailResponse;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatentServiceTest {

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PatentLegalStatusRepository patentLegalStatusRepository;

    @Mock
    private PatentAnnuityRepository patentAnnuityRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReportRepository reportRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PatentService patentService;

    @Test
    void createPreservesTitleAndApplicationNumber() {
        when(patentRepository.save(any(Patent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatentDetailResponse response = patentService.create(createRequest("  Patent Title  ", " 10-1234 "));

        verify(patentRepository).existsByApplicationNumber(" 10-1234 ");
        verify(patentRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getTitle().equals("  Patent Title  ")
                        && saved.getApplicationNumber().equals(" 10-1234 ")
                        && saved.getInitialDepartment().equals(" Initial Department ")
        ));
        assertThat(response.title()).isEqualTo("  Patent Title  ");
        assertThat(response.initialDepartment()).isEqualTo(" Initial Department ");
        assertThat(response.relatedProducts()).containsExactly("Product");
        assertThat(response.keywords()).containsExactly("Keyword");
    }

    @Test
    void createRejectsDuplicateApplicationNumber() {
        when(patentRepository.existsByApplicationNumber("APP-1")).thenReturn(true);

        assertPatentError(
                () -> patentService.create(createRequest("Patent", "APP-1")),
                ErrorCode.DUPLICATE_APPLICATION_NUMBER
        );

        verify(patentRepository, never()).save(any());
    }

    @Test
    void createRejectsValuesThatCannotBeSerializedAsJson() throws Exception {
        doThrow(new JsonProcessingException("cannot serialize") {
        }).when(objectMapper).writeValueAsString(any());

        assertThatThrownBy(() -> patentService.create(createRequest("Patent", "APP-1")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(patentRepository, never()).save(any());
    }

    @Test
    void getRejectsMissingPatent() {
        when(patentRepository.findById(1L)).thenReturn(Optional.empty());

        assertPatentError(() -> patentService.get(1L), ErrorCode.PATENT_NOT_FOUND);
    }

    @Test
    void getReturnsInternalErrorWhenStoredListJsonIsInvalid() {
        Patent patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .relatedProducts("not-json")
                .build();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));

        assertThatThrownBy(() -> patentService.get(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void getAllWithoutKeywordUsesPagedFindAll() {
        Patent patent = Patent.builder().title("Patent").applicationNumber("APP-1").build();
        Pageable pageable = PageRequest.of(0, 20);
        Pageable sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        when(patentRepository.findAll(sortedPageable)).thenReturn(new PageImpl<>(List.of(patent), sortedPageable, 1));

        Page<?> result = patentService.getAll("  ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(patentRepository).findAll(sortedPageable);
    }

    @Test
    void getAllWithKeywordSearchesTitle() {
        Patent patent = Patent.builder().title("Patent").applicationNumber("APP-1").build();
        Pageable pageable = PageRequest.of(0, 20);
        Pageable sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        when(patentRepository.findByTitleContainingIgnoreCase("patent", sortedPageable))
                .thenReturn(new PageImpl<>(List.of(patent), sortedPageable, 1));

        Page<?> result = patentService.getAll(" patent ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(patentRepository).findByTitleContainingIgnoreCase("patent", sortedPageable);
    }

    @Test
    void updateReplacesPatentFieldsAndPreservesApplicationNumber() {
        Patent patent = Patent.builder()
                .title("Old Title")
                .applicationNumber("OLD-APP")
                .build();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));

        PatentDetailResponse response = patentService.update(1L, updateRequest("  Updated Title  ", " NEW-APP "));

        assertThat(response.title()).isEqualTo("  Updated Title  ");
        assertThat(response.applicationNumber()).isEqualTo(" NEW-APP ");
        assertThat(response.registrationNumber()).isEqualTo("REG-1");
        assertThat(response.applicationDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.applicant()).isEqualTo("Applicant");
        assertThat(response.initialDepartment()).isEqualTo("Initial Department");
        assertThat(response.relatedProducts()).containsExactly("Product");
        assertThat(response.keywords()).containsExactly("Keyword");
        assertThat(response.overview()).isEqualTo("Overview");
        verify(patentRepository).existsByApplicationNumber(" NEW-APP ");
    }

    @Test
    void updateRejectsDuplicateChangedApplicationNumber() {
        Patent patent = Patent.builder()
                .title("Old Title")
                .applicationNumber("OLD-APP")
                .build();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));
        when(patentRepository.existsByApplicationNumber("NEW-APP")).thenReturn(true);

        assertPatentError(
                () -> patentService.update(1L, updateRequest("Updated Title", "NEW-APP")),
                ErrorCode.DUPLICATE_APPLICATION_NUMBER
        );

        assertThat(patent.getTitle()).isEqualTo("Old Title");
        assertThat(patent.getApplicationNumber()).isEqualTo("OLD-APP");
    }

    @Test
    void updateWithUnchangedApplicationNumberDoesNotCheckDuplicate() {
        Patent patent = Patent.builder()
                .title("Old Title")
                .applicationNumber("APP-1")
                .build();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));

        PatentDetailResponse response = patentService.update(1L, updateRequest("Updated Title", "APP-1"));

        assertThat(response.title()).isEqualTo("Updated Title");
        verify(patentRepository, never()).existsByApplicationNumber(any());
    }

    @Test
    void updateRejectsMissingPatent() {
        when(patentRepository.findById(1L)).thenReturn(Optional.empty());

        assertPatentError(() -> patentService.update(1L, null), ErrorCode.PATENT_NOT_FOUND);
    }

    @Test
    void deleteRemovesExistingPatent() {
        when(patentRepository.existsById(1L)).thenReturn(true);

        patentService.delete(1L);

        InOrder deletionOrder = inOrder(patentLegalStatusRepository, patentAnnuityRepository, reviewRepository, reportRepository, patentRepository);
        deletionOrder.verify(patentLegalStatusRepository).deleteAllByPatentId(1L);
        deletionOrder.verify(patentAnnuityRepository).deleteAllByPatentId(1L);
        deletionOrder.verify(reviewRepository).deleteAllByPatentId(1L);
        deletionOrder.verify(reportRepository).deleteAllByPatentId(1L);
        deletionOrder.verify(patentRepository).deleteById(1L);
    }

    @Test
    void deleteRejectsMissingPatent() {
        when(patentRepository.existsById(1L)).thenReturn(false);

        assertPatentError(() -> patentService.delete(1L), ErrorCode.PATENT_NOT_FOUND);

        verify(patentLegalStatusRepository, never()).deleteAllByPatentId(1L);
        verify(patentAnnuityRepository, never()).deleteAllByPatentId(1L);
        verify(reviewRepository, never()).deleteAllByPatentId(1L);
        verify(reportRepository, never()).deleteAllByPatentId(1L);
        verify(patentRepository, never()).deleteById(1L);
    }

    private PatentCreateRequest createRequest(String title, String applicationNumber) {
        return new PatentCreateRequest(
                title,
                applicationNumber,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("Product"),
                null,
                null,
                null,
                " Initial Department ",
                List.of("Keyword"),
                null,
                null
        );
    }

    private PatentUpdateRequest updateRequest(String title, String applicationNumber) {
        return new PatentUpdateRequest(
                title,
                applicationNumber,
                "REG-1",
                "PUB-1",
                "ANN-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4),
                "IPC",
                "CPC",
                "Applicant",
                "Inventor",
                LocalDate.of(2046, 1, 1),
                3,
                "pdf-key",
                "management",
                "business",
                "tech",
                List.of("Product"),
                "KR",
                true,
                "Joint Applicant",
                "Initial Department",
                List.of("Keyword"),
                "Overview",
                "Core Content"
        );
    }

    private void assertPatentError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(PatentException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
