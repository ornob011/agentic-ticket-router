package com.dsi.support.agenticrouter.service.knowledge;

import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
@Slf4j
public class LibraryLexicalScorer {

    private final Analyzer analyzer = new EnglishAnalyzer();

    public double score(
        String query,
        String title,
        String[] keywords
    ) {
        Set<String> queryTerms = analyze(
            query
        );

        if (queryTerms.isEmpty()) {
            return 0D;
        }

        Set<String> documentTerms = new HashSet<>(
            analyze(
                title
            )
        );

        if (Objects.nonNull(keywords)) {
            for (String keyword : keywords) {
                documentTerms.addAll(analyze(
                    keyword
                ));
            }
        }

        if (documentTerms.isEmpty()) {
            return 0D;
        }

        long overlapCount = queryTerms.stream()
                                      .filter(documentTerms::contains)
                                      .count();

        double lexicalScore = (double) overlapCount / (double) queryTerms.size();

        log.debug(
            "LexicalScore({}) Outcome(queryTerms:{},documentTerms:{},overlapCount:{},score:{})",
            OperationalLogContext.PHASE_COMPLETE,
            queryTerms.size(),
            documentTerms.size(),
            overlapCount,
            lexicalScore
        );

        return Math.clamp(
            lexicalScore,
            0D,
            1D
        );
    }

    private Set<String> analyze(
        String text
    ) {
        String normalized = StringUtils.trimToEmpty(
            text
        );

        if (StringUtils.isBlank(normalized)) {
            return Set.of();
        }

        Set<String> terms = new HashSet<>();

        try (
            TokenStream tokenStream = analyzer.tokenStream(
                "content",
                normalized
            )
        ) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(
                CharTermAttribute.class
            );

            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                String term = StringUtils.trimToNull(
                    termAttribute.toString()
                );

                if (Objects.nonNull(term)) {
                    terms.add(term);
                }
            }

            tokenStream.end();
            return terms;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to analyze text using lexical analyzer", exception);
        }
    }
}
