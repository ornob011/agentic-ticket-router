package com.dsi.support.agenticrouter.service.learning;

import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Slf4j
public class PatternKeywordExtractor {

    private static final int MIN_KEYWORD_LENGTH = 4;
    private static final int MAX_KEYWORDS_PER_PATTERN = 10;
    private static final String ANALYZER_FIELD = "subject";

    private final EnglishAnalyzer englishAnalyzer = new EnglishAnalyzer();

    public List<String> extractKeywords(
        String text
    ) {
        String normalizedText = StringNormalizationUtils.trimToNull(text);
        if (normalizedText == null) {
            return List.of();
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        int tokenCount = 0;

        try (
            TokenStream tokenStream = englishAnalyzer.tokenStream(
                ANALYZER_FIELD,
                new StringReader(normalizedText)
            )
        ) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(
                CharTermAttribute.class
            );
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                tokenCount++;
                String token = termAttribute.toString();

                if (token.length() < MIN_KEYWORD_LENGTH) {
                    continue;
                }

                keywords.add(token);
                if (keywords.size() >= MAX_KEYWORDS_PER_PATTERN) {
                    break;
                }
            }

            tokenStream.end();
        } catch (IOException ioException) {
            log.error(
                "KeywordExtract({}) Outcome(reason:{},inputLength:{})",
                OperationalLogContext.PHASE_FAIL,
                "analyzer_io_error",
                StringUtils.length(text),
                ioException
            );
            throw new IllegalStateException(
                "Keyword extraction failed",
                ioException
            );
        }

        log.debug(
            "KeywordExtract({}) Outcome(inputLength:{},tokenCount:{},keywordCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            StringUtils.length(text),
            tokenCount,
            keywords.size()
        );

        return List.copyOf(keywords);
    }

    @PreDestroy
    public void destroy() {
        englishAnalyzer.close();
    }
}
