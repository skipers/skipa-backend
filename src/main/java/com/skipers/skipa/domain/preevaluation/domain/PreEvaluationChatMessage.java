package com.skipers.skipa.domain.preevaluation.domain;

import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pre_evaluation_chat_messages")
public class PreEvaluationChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_evaluation_id", nullable = false)
    private PreEvaluation preEvaluation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private PreEvaluationChatRole role;

    @Column(name = "content", columnDefinition = "text", nullable = false)
    private String content;

    @Builder
    private PreEvaluationChatMessage(
            PreEvaluation preEvaluation,
            PreEvaluationChatRole role,
            String content
    ) {
        this.preEvaluation = preEvaluation;
        this.role = role;
        this.content = content;
    }
}
