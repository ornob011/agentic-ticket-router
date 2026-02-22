package com.dsi.support.agenticrouter.service.learning;

import com.dsi.support.agenticrouter.dto.PatternHint;
import com.dsi.support.agenticrouter.entity.RoutingPattern;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.repository.RoutingPatternRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingPatternMatcherService {

    private static final int MAX_PATTERNS = 5;

    private final RoutingPatternRepository routingPatternRepository;
    private final PatternKeywordExtractor keywordExtractor;

    @Transactional(readOnly = true)
    @Cacheable(value = "patternHints", key = "#category.name() + '_' + #subject.hashCode()")
    public List<PatternHint> findRelevantPatterns(
        TicketCategory category,
        String subject
    ) {
        log.debug(
            "RoutingPatternMatch({}) Outcome(category:{},subjectLength:{})",
            OperationalLogContext.PHASE_START,
            category,
            StringUtils.length(subject)
        );

        Map<String, RoutingPattern> merged = new LinkedHashMap<>();

        if (category != null) {
            routingPatternRepository.findByCategoryAndActiveTrueOrderBySuccessCountDesc(category)
                                    .stream()
                                    .filter(RoutingPattern::isReliable)
                                    .forEach(routingPattern -> merged.putIfAbsent(patternKey(routingPattern), routingPattern));
        }

        if (StringUtils.isNotBlank(subject)) {
            List<String> keywords = keywordExtractor.extractKeywords(subject);
            if (!keywords.isEmpty()) {
                routingPatternRepository.findByKeywordsIn(keywords)
                                        .stream()
                                        .filter(RoutingPattern::isReliable)
                                        .forEach(routingPattern -> merged.putIfAbsent(patternKey(routingPattern), routingPattern));
            }
        }

        List<PatternHint> hints = merged.values().stream()
                                        .sorted(Comparator.comparingDouble(RoutingPattern::getSuccessRate).reversed())
                                        .limit(MAX_PATTERNS)
                                        .map(this::toPatternHint)
                                        .toList();

        log.debug(
            "RoutingPatternMatch({}) Outcome(category:{},subjectLength:{},matched:{})",
            OperationalLogContext.PHASE_COMPLETE,
            category,
            StringUtils.length(subject),
            hints.size()
        );

        return hints;
    }

    private String patternKey(
        RoutingPattern pattern
    ) {
        return pattern.getCategory().name() + "|" + pattern.getSuccessfulAction().name();
    }

    private PatternHint toPatternHint(
        RoutingPattern pattern
    ) {
        int successCount = pattern.getSuccessCount() != null ? pattern.getSuccessCount() : 0;
        int failureCount = pattern.getFailureCount() != null ? pattern.getFailureCount() : 0;

        return new PatternHint(
            pattern.getCategory().name(),
            pattern.getSuccessfulAction().name(),
            pattern.getSuccessRate(),
            successCount + failureCount,
            pattern.getKeywords()
        );
    }
}
