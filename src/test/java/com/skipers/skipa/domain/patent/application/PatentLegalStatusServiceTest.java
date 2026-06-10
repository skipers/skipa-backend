package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
import com.skipers.skipa.domain.patent.dto.request.PatentLegalStatusCreateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentLegalStatusResponse;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PatentLegalStatusServiceTest {

    @Mock
    private PatentLegalStatusRepository patentLegalStatusRepository;

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private BusinessPatentAccessValidator businessPatentAccessValidator;

    @Mock
    private ApprovedPatentValidator approvedPatentValidator;

    @InjectMocks
    private PatentLegalStatusService patentLegalStatusService;

    @BeforeEach
    void setUp() {
        lenient().when(approvedPatentValidator.getApprovedPatent(any()))
                .thenAnswer(invocation -> {
                    Optional<Patent> patent = patentRepository.findById(invocation.getArgument(0));
                    return patent != null && patent.isPresent() ? patent.get() : patent();
                });
    }

    @Test
    void createSavesLegalStatusHistory() {
        Patent patent = patent();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));
        when(patentLegalStatusRepository.save(any(PatentLegalStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatentLegalStatusResponse response = patentLegalStatusService.create(1L, request("REGISTERED"));

        assertThat(response.patentId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("REGISTERED");
        assertThat(response.changedAt()).isEqualTo(LocalDate.of(2026, 5, 29));
        verify(patentLegalStatusRepository).save(org.mockito.ArgumentMatchers.argThat(status ->
                status.getPatent() == patent && status.getStatus() == PatentLegalStatusType.REGISTERED
        ));
    }

    @Test
    void createSupportsAppliedStatus() {
        Patent patent = patent();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));
        when(patentLegalStatusRepository.save(any(PatentLegalStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatentLegalStatusResponse response = patentLegalStatusService.create(1L, request("APPLIED"));

        assertThat(response.status()).isEqualTo("APPLIED");
        verify(patentLegalStatusRepository).save(org.mockito.ArgumentMatchers.argThat(status ->
                status.getPatent() == patent && status.getStatus() == PatentLegalStatusType.APPLIED
        ));
    }

    @Test
    void createRejectsMissingPatent() {
        when(approvedPatentValidator.getApprovedPatent(1L))
                .thenThrow(new PatentException(ErrorCode.PATENT_NOT_FOUND));

        assertPatentError(() -> patentLegalStatusService.create(1L, request("REGISTERED")), ErrorCode.PATENT_NOT_FOUND);

        verify(patentLegalStatusRepository, never()).save(any());
    }

    @Test
    void createRejectsInvalidStatus() {
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent()));

        assertThatThrownBy(() -> patentLegalStatusService.create(1L, request("보류")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(patentLegalStatusRepository, never()).save(any());
    }

    @Test
    void getAllValidatesAccessAndUsesDescendingIdSort() {
        User user = legalUser();
        PatentLegalStatus legalStatus = PatentLegalStatus.builder()
                .patent(patent())
                .status(PatentLegalStatusType.PUBLISHED)
                .changedAt(LocalDate.of(2026, 5, 29))
                .build();
        ReflectionTestUtils.setField(legalStatus, "id", 10L);
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        when(patentLegalStatusRepository.findByPatentId(1L, sortedPageable))
                .thenReturn(new PageImpl<>(List.of(legalStatus), sortedPageable, 1));

        assertThat(patentLegalStatusService.getAll(user, 1L, pageable).getContent())
                .extracting(PatentLegalStatusResponse::id)
                .containsExactly(10L);
        verify(businessPatentAccessValidator).validate(user, 1L);
        verify(patentLegalStatusRepository).findByPatentId(1L, sortedPageable);
    }

    @Test
    void getAllRejectsMissingPatent() {
        User user = legalUser();
        when(approvedPatentValidator.getApprovedPatent(1L))
                .thenThrow(new PatentException(ErrorCode.PATENT_NOT_FOUND));

        assertPatentError(
                () -> patentLegalStatusService.getAll(user, 1L, PageRequest.of(0, 20)),
                ErrorCode.PATENT_NOT_FOUND
        );

        verify(businessPatentAccessValidator).validate(user, 1L);
        verify(patentLegalStatusRepository, never()).findByPatentId(any(), any());
    }

    private Patent patent() {
        Patent patent = Patent.builder().title("Patent").applicationNumber("APP-1").build();
        ReflectionTestUtils.setField(patent, "id", 1L);
        return patent;
    }

    private PatentLegalStatusCreateRequest request(String status) {
        return new PatentLegalStatusCreateRequest(status, LocalDate.of(2026, 5, 29));
    }

    private User legalUser() {
        return User.createActive("legal", "Legal", "legal@example.com", "password", UserRole.LEGAL, null);
    }

    private void assertPatentError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(PatentException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
