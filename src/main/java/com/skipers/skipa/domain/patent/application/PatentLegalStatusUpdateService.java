package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatentLegalStatusUpdateService {

    private static final Set<PatentLegalStatusType> ACTIVE_STATUSES = Set.of(
            PatentLegalStatusType.APPLIED,
            PatentLegalStatusType.PUBLISHED,
            PatentLegalStatusType.REGISTERED
    );

    private final PatentRepository patentRepository;
    private final PatentAnnuityRepository patentAnnuityRepository;
    private final PatentLegalStatusRepository patentLegalStatusRepository;

    @Transactional
    public int updateAdministrativeStatuses() {
        return updateAdministrativeStatuses(LocalDate.now());
    }

    @Transactional
    int updateAdministrativeStatuses(LocalDate today) {
        List<PatentLegalStatus> legalStatuses = new ArrayList<>();
        List<PatentLegalStatus> expiredStatuses = expirePatents(today);
        legalStatuses.addAll(expiredStatuses);
        legalStatuses.addAll(abandonOverdueAnnuities(today, patentIdsByLegalStatus(expiredStatuses)));

        if (!legalStatuses.isEmpty()) {
            patentLegalStatusRepository.saveAll(legalStatuses);
        }
        return legalStatuses.size();
    }

    private List<PatentLegalStatus> expirePatents(LocalDate today) {
        List<Patent> patents = patentRepository.findByExpiryDateBefore(today);
        Map<Long, PatentLegalStatus> latestStatuses = latestStatuses(patentIds(patents));

        List<PatentLegalStatus> legalStatuses = new ArrayList<>();
        for (Patent patent : patents) {
            if (canTransition(latestStatuses.get(patent.getId()))) {
                legalStatuses.add(legalStatus(patent, PatentLegalStatusType.EXPIRED, today));
            }
        }
        return legalStatuses;
    }

    private List<PatentLegalStatus> abandonOverdueAnnuities(LocalDate today, Set<Long> excludedPatentIds) {
        List<PatentAnnuity> annuities = patentAnnuityRepository.findByStatusAndDueDateBefore(
                PatentAnnuityStatus.UNPAID,
                today
        );
        Map<Long, PatentLegalStatus> latestStatuses = latestStatuses(patentIdsByAnnuity(annuities));
        Set<Long> abandonedPatentIds = new HashSet<>();

        List<PatentLegalStatus> legalStatuses = new ArrayList<>();
        for (PatentAnnuity annuity : annuities) {
            annuity.abandon();

            Patent patent = annuity.getPatent();
            if (!excludedPatentIds.contains(patent.getId())
                    && abandonedPatentIds.add(patent.getId())
                    && canTransition(latestStatuses.get(patent.getId()))) {
                legalStatuses.add(legalStatus(patent, PatentLegalStatusType.ABANDONED, today));
            }
        }
        return legalStatuses;
    }

    private Map<Long, PatentLegalStatus> latestStatuses(Collection<Long> patentIds) {
        if (patentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, PatentLegalStatus> latestByPatentId = new HashMap<>();
        for (PatentLegalStatus legalStatus : patentLegalStatusRepository.findByPatentIdIn(patentIds)) {
            Long patentId = legalStatus.getPatent().getId();
            PatentLegalStatus current = latestByPatentId.get(patentId);
            if (current == null || compareLegalStatusRecency(legalStatus, current) > 0) {
                latestByPatentId.put(patentId, legalStatus);
            }
        }
        return latestByPatentId;
    }

    private boolean canTransition(PatentLegalStatus latestStatus) {
        return latestStatus == null || ACTIVE_STATUSES.contains(latestStatus.getStatus());
    }

    private PatentLegalStatus legalStatus(Patent patent, PatentLegalStatusType status, LocalDate changedAt) {
        return PatentLegalStatus.builder()
                .patent(patent)
                .status(status)
                .changedAt(changedAt)
                .build();
    }

    private Set<Long> patentIds(List<Patent> patents) {
        Set<Long> patentIds = new HashSet<>();
        for (Patent patent : patents) {
            patentIds.add(patent.getId());
        }
        return patentIds;
    }

    private Set<Long> patentIdsByAnnuity(List<PatentAnnuity> annuities) {
        Set<Long> patentIds = new HashSet<>();
        for (PatentAnnuity annuity : annuities) {
            patentIds.add(annuity.getPatent().getId());
        }
        return patentIds;
    }

    private Set<Long> patentIdsByLegalStatus(List<PatentLegalStatus> legalStatuses) {
        Set<Long> patentIds = new HashSet<>();
        for (PatentLegalStatus legalStatus : legalStatuses) {
            patentIds.add(legalStatus.getPatent().getId());
        }
        return patentIds;
    }

    private int compareLegalStatusRecency(PatentLegalStatus left, PatentLegalStatus right) {
        Comparator<PatentLegalStatus> comparator = Comparator
                .comparing(PatentLegalStatus::getChangedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(PatentLegalStatus::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
        return comparator.compare(left, right);
    }
}
