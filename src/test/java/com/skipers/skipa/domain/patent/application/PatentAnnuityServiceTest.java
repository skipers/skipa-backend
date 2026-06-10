package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.patent.dto.request.PatentAnnuityCreateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentAnnuityResponse;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
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

@ExtendWith(MockitoExtension.class)
class PatentAnnuityServiceTest {

    @Mock
    private PatentAnnuityRepository patentAnnuityRepository;

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private BusinessPatentAccessValidator businessPatentAccessValidator;

    @InjectMocks
    private PatentAnnuityService patentAnnuityService;

    @Test
    void createSavesAnnuityHistory() {
        Patent patent = patent();
        PatentAnnuity unpaidAnnuity = PatentAnnuity.builder()
                .patent(patent)
                .startYear(3)
                .dueDate(LocalDate.of(2026, 12, 31))
                .status(PatentAnnuityStatus.UNPAID)
                .build();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));
        when(patentAnnuityRepository.findFirstByPatentIdAndStatusOrderByStartYearDescIdDesc(
                1L,
                PatentAnnuityStatus.UNPAID
        )).thenReturn(Optional.of(unpaidAnnuity));
        when(patentAnnuityRepository.save(any(PatentAnnuity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatentAnnuityResponse response = patentAnnuityService.create(1L, request());

        assertThat(response.patentId()).isEqualTo(1L);
        assertThat(response.startYear()).isEqualTo(3);
        assertThat(response.endYear()).isEqualTo(4);
        assertThat(response.status()).isEqualTo("PAID");
        assertThat(response.amount()).isEqualTo(100_000);
        assertThat(response.paidDate()).isEqualTo(LocalDate.now());
        verify(patentAnnuityRepository).existsByPatentIdAndStartYear(1L, 5);
        verify(patentAnnuityRepository).save(org.mockito.ArgumentMatchers.argThat(annuity ->
                annuity.getPatent() == patent
                        && annuity.getStartYear().equals(5)
                        && annuity.getEndYear() == null
                        && annuity.getDueDate().equals(LocalDate.of(2028, 12, 31))
                        && annuity.getStatus() == PatentAnnuityStatus.UNPAID
        ));
    }

    @Test
    void createRejectsMissingPatent() {
        when(patentRepository.findById(1L)).thenReturn(Optional.empty());

        assertPatentError(() -> patentAnnuityService.create(1L, request()), ErrorCode.PATENT_NOT_FOUND);

        verify(patentAnnuityRepository, never()).save(any());
    }

    @Test
    void createRejectsMissingUnpaidAnnuity() {
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent()));
        when(patentAnnuityRepository.findFirstByPatentIdAndStatusOrderByStartYearDescIdDesc(
                1L,
                PatentAnnuityStatus.UNPAID
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patentAnnuityService.create(1L, request()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PATENT_ANNUITY_NOT_FOUND));

        verify(patentAnnuityRepository, never()).save(any());
    }

    @Test
    void getAllValidatesAccessAndUsesDescendingIdSort() {
        User user = legalUser();
        PatentAnnuity annuity = PatentAnnuity.builder()
                .patent(patent())
                .startYear(3)
                .status(PatentAnnuityStatus.UNPAID)
                .build();
        ReflectionTestUtils.setField(annuity, "id", 10L);
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        when(patentRepository.existsById(1L)).thenReturn(true);
        when(patentAnnuityRepository.findByPatentId(1L, sortedPageable))
                .thenReturn(new PageImpl<>(List.of(annuity), sortedPageable, 1));

        assertThat(patentAnnuityService.getAll(user, 1L, pageable).getContent())
                .extracting(PatentAnnuityResponse::id)
                .containsExactly(10L);
        verify(businessPatentAccessValidator).validate(user, 1L);
        verify(patentAnnuityRepository).findByPatentId(1L, sortedPageable);
    }

    @Test
    void getAllRejectsMissingPatent() {
        User user = legalUser();
        when(patentRepository.existsById(1L)).thenReturn(false);

        assertPatentError(
                () -> patentAnnuityService.getAll(user, 1L, PageRequest.of(0, 20)),
                ErrorCode.PATENT_NOT_FOUND
        );

        verify(businessPatentAccessValidator).validate(user, 1L);
        verify(patentAnnuityRepository, never()).findByPatentId(any(), any());
    }

    private Patent patent() {
        Patent patent = Patent.builder().title("Patent").applicationNumber("APP-1").build();
        ReflectionTestUtils.setField(patent, "id", 1L);
        return patent;
    }

    private PatentAnnuityCreateRequest request() {
        return new PatentAnnuityCreateRequest(
                2,
                100_000
        );
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
