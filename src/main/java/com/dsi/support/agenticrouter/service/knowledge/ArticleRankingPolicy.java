package com.dsi.support.agenticrouter.service.knowledge;

import com.dsi.support.agenticrouter.configuration.RagConfiguration;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleRankingPolicy {

    private static final double CATEGORY_MATCH_BONUS = 0.05D;

    private final RagConfiguration ragConfiguration;

    public double rank(
        double vectorScore,
        Integer priority,
        Double successRate,
        String category,
        String categoryHint
    ) {
        log.debug(
            "ArticleRank({}) Outcome(vectorScore:{},priority:{},successRate:{},category:{},categoryHint:{})",
            OperationalLogContext.PHASE_START,
            vectorScore,
            priority,
            successRate,
            category,
            categoryHint
        );

        double denseSimilarityScore = clamp01(
            vectorScore
        );

        double priorityNormalizedScore = clamp01(
            mapOrDefault(
                priority,
                0D,
                value -> value.doubleValue() / 10D
            )
        );

        double successRateScore = clamp01(
            mapOrDefault(
                successRate,
                0D,
                value -> value
            )
        );

        double categoryMatchBonus = categoryMatchBonus(category,
            categoryHint
        );

        double weightedScore = (denseSimilarityScore * ragConfiguration.getDenseWeight())
                               + (priorityNormalizedScore * ragConfiguration.getPriorityWeight())
                               + (successRateScore * ragConfiguration.getSuccessRateWeight())
                               + categoryMatchBonus;

        double finalScore = clamp01(
            weightedScore
        );

        log.debug(
            "ArticleRank({}) Outcome(dense:{},priorityNorm:{},successNorm:{},categoryBonus:{},finalScore:{})",
            OperationalLogContext.PHASE_COMPLETE,
            denseSimilarityScore,
            priorityNormalizedScore,
            successRateScore,
            categoryMatchBonus,
            finalScore
        );

        return finalScore;
    }

    private double clamp01(
        double value
    ) {
        return Math.clamp(
            value,
            0D,
            1D
        );
    }

    private double categoryMatchBonus(
        String category,
        String categoryHint
    ) {
        String normalizedHint = normalize(
            categoryHint
        );

        String normalizedCategory = normalize(
            category
        );

        return select(
            StringUtils.isNotBlank(normalizedHint) && Objects.equals(normalizedHint, normalizedCategory),
            CATEGORY_MATCH_BONUS,
            0D
        );
    }

    private String normalize(
        String value
    ) {
        return StringUtils.trimToEmpty(value).toLowerCase();
    }

    private <T> double mapOrDefault(
        T value,
        double defaultValue,
        DoubleMapper<T> mapper
    ) {
        return select(
            Objects.nonNull(value),
            mapper.map(value),
            defaultValue
        );
    }

    private double select(
        boolean condition,
        double whenTrue,
        double whenFalse
    ) {
        return condition ? whenTrue : whenFalse;
    }

    @FunctionalInterface
    private interface DoubleMapper<T> {
        double map(T value);
    }
}
