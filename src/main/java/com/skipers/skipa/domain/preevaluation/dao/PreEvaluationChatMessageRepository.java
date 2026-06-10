package com.skipers.skipa.domain.preevaluation.dao;

import com.skipers.skipa.domain.preevaluation.domain.PreEvaluationChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreEvaluationChatMessageRepository extends JpaRepository<PreEvaluationChatMessage, Long> {

    List<PreEvaluationChatMessage> findByPreEvaluationIdOrderByCreatedAtAsc(Long preEvaluationId);

    void deleteAllByPreEvaluationId(Long preEvaluationId);
}
