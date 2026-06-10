package com.skipers.skipa.domain.preevaluation.application;

import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;

public interface PreEvaluationGenerationPublisher {

    void publish(PreEvaluation preEvaluation);
}
