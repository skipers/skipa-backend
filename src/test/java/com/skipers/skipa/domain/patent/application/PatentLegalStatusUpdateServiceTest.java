package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class PatentLegalStatusUpdateServiceTest {

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private PatentAnnuityRepository patentAnnuityRepository;

    @Mock
    private PatentLegalStatusRepository patentLegalStatusRepository;

    @InjectMocks
    private PatentLegalStatusUpdateService patentLegalStatusUpdateService;

    @Test
    void updateAdministrativeStatusesExpiresPatentsPastExpiryDate() {
        LocalDate today = LocalDate.of(2026, 6, 10);
        Patent activePatent = patent(1L, "APP-1", today.minusDays(1));
        Patent terminalPatent = patent(2L, "APP-2", today.minusDays(2));
        PatentLegalStatus activeStatus = legalStatus(10L, activePatent, PatentLegalStatusType.REGISTERED, today.minusDays(10));
        PatentLegalStatus terminalStatus = legalStatus(20L, terminalPatent, PatentLegalStatusType.ABANDONED, today.minusDays(10));
        when(patentRepository.findByExpiryDateBefore(today)).thenReturn(List.of(activePatent, terminalPatent));
        when(patentLegalStatusRepository.findByPatentIdIn(anyCollection()))
                .thenReturn(List.of(activeStatus, terminalStatus));
        when(patentAnnuityRepository.findByStatusAndDueDateBefore(PatentAnnuityStatus.UNPAID, today))
                .thenReturn(List.of());

        int updatedCount = patentLegalStatusUpdateService.updateAdministrativeStatuses(today);

        assertThat(updatedCount).isEqualTo(1);
        ArgumentCaptor<Iterable<PatentLegalStatus>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(patentLegalStatusRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(PatentLegalStatus::getPatent, PatentLegalStatus::getStatus, PatentLegalStatus::getChangedAt)
                .containsExactly(tuple(activePatent, PatentLegalStatusType.EXPIRED, today));
    }

    @Test
    void updateAdministrativeStatusesAbandonsOverdueUnpaidAnnuities() {
        LocalDate today = LocalDate.of(2026, 6, 10);
        Patent patent = patent(1L, "APP-1", today.plusYears(1));
        PatentAnnuity firstAnnuity = annuity(10L, patent, today.minusDays(1));
        PatentAnnuity secondAnnuity = annuity(20L, patent, today.minusDays(2));
        PatentLegalStatus activeStatus = legalStatus(10L, patent, PatentLegalStatusType.REGISTERED, today.minusDays(10));
        when(patentRepository.findByExpiryDateBefore(today)).thenReturn(List.of());
        when(patentAnnuityRepository.findByStatusAndDueDateBefore(PatentAnnuityStatus.UNPAID, today))
                .thenReturn(List.of(firstAnnuity, secondAnnuity));
        when(patentLegalStatusRepository.findByPatentIdIn(anyCollection())).thenReturn(List.of(activeStatus));

        int updatedCount = patentLegalStatusUpdateService.updateAdministrativeStatuses(today);

        assertThat(updatedCount).isEqualTo(1);
        assertThat(firstAnnuity.getStatus()).isEqualTo(PatentAnnuityStatus.ABANDONED);
        assertThat(secondAnnuity.getStatus()).isEqualTo(PatentAnnuityStatus.ABANDONED);
        ArgumentCaptor<Iterable<PatentLegalStatus>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(patentLegalStatusRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(PatentLegalStatus::getPatent, PatentLegalStatus::getStatus, PatentLegalStatus::getChangedAt)
                .containsExactly(tuple(patent, PatentLegalStatusType.ABANDONED, today));
    }

    @Test
    void updateAdministrativeStatusesKeepsExpiredStatusWhenExpiryAndAnnuityAreBothOverdue() {
        LocalDate today = LocalDate.of(2026, 6, 10);
        Patent patent = patent(1L, "APP-1", today.minusDays(1));
        PatentAnnuity annuity = annuity(10L, patent, today.minusDays(1));
        PatentLegalStatus activeStatus = legalStatus(10L, patent, PatentLegalStatusType.REGISTERED, today.minusDays(10));
        when(patentRepository.findByExpiryDateBefore(today)).thenReturn(List.of(patent));
        when(patentAnnuityRepository.findByStatusAndDueDateBefore(PatentAnnuityStatus.UNPAID, today))
                .thenReturn(List.of(annuity));
        when(patentLegalStatusRepository.findByPatentIdIn(anyCollection()))
                .thenReturn(List.of(activeStatus), List.of(activeStatus));

        int updatedCount = patentLegalStatusUpdateService.updateAdministrativeStatuses(today);

        assertThat(updatedCount).isEqualTo(1);
        assertThat(annuity.getStatus()).isEqualTo(PatentAnnuityStatus.ABANDONED);
        ArgumentCaptor<Iterable<PatentLegalStatus>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(patentLegalStatusRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(PatentLegalStatus::getPatent, PatentLegalStatus::getStatus, PatentLegalStatus::getChangedAt)
                .containsExactly(tuple(patent, PatentLegalStatusType.EXPIRED, today));
    }

    @Test
    void updateAdministrativeStatusesDoesNotSaveWhenNothingChanged() {
        LocalDate today = LocalDate.of(2026, 6, 10);
        when(patentRepository.findByExpiryDateBefore(today)).thenReturn(List.of());
        when(patentAnnuityRepository.findByStatusAndDueDateBefore(PatentAnnuityStatus.UNPAID, today))
                .thenReturn(List.of());

        int updatedCount = patentLegalStatusUpdateService.updateAdministrativeStatuses(today);

        assertThat(updatedCount).isZero();
        verify(patentLegalStatusRepository, never()).saveAll(anyCollection());
    }

    private Patent patent(Long id, String applicationNumber, LocalDate expiryDate) {
        Patent patent = Patent.builder()
                .title("Patent " + id)
                .applicationNumber(applicationNumber)
                .expiryDate(expiryDate)
                .build();
        ReflectionTestUtils.setField(patent, "id", id);
        return patent;
    }

    private PatentAnnuity annuity(Long id, Patent patent, LocalDate dueDate) {
        PatentAnnuity annuity = PatentAnnuity.builder()
                .patent(patent)
                .startYear(1)
                .endYear(1)
                .dueDate(dueDate)
                .status(PatentAnnuityStatus.UNPAID)
                .build();
        ReflectionTestUtils.setField(annuity, "id", id);
        return annuity;
    }

    private PatentLegalStatus legalStatus(
            Long id,
            Patent patent,
            PatentLegalStatusType status,
            LocalDate changedAt
    ) {
        PatentLegalStatus legalStatus = PatentLegalStatus.builder()
                .patent(patent)
                .status(status)
                .changedAt(changedAt)
                .build();
        ReflectionTestUtils.setField(legalStatus, "id", id);
        return legalStatus;
    }
}
