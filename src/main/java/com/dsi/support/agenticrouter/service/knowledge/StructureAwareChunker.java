package com.dsi.support.agenticrouter.service.knowledge;

import com.dsi.support.agenticrouter.configuration.RagConfiguration;
import com.dsi.support.agenticrouter.entity.KnowledgeArticle;
import com.dsi.support.agenticrouter.enums.KnowledgeChunkType;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.github.slugify.Slugify;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class StructureAwareChunker {

    private final RagConfiguration ragConfiguration;
    private final OpenNlpTokenizer openNlpTokenizer;
    private final Slugify slugify = Slugify.builder().build();
    private final Parser markdownParser = Parser.builder().build();

    public List<ChunkPayload> chunkArticle(
        KnowledgeArticle article
    ) {
        log.debug(
            "StructureAwareChunker({}) Outcome(articleId:{},title:{},contentLength:{})",
            OperationalLogContext.PHASE_START,
            article.getId(),
            article.getTitle(),
            StringUtils.length(article.getContent())
        );

        List<ChunkPayload> chunks = new ArrayList<>();

        addTitleChunk(
            chunks,
            article.getTitle()
        );

        Node document = markdownParser.parse(StringUtils.defaultString(article.getContent()));

        processMarkdownNode(
            chunks,
            document,
            "content"
        );

        List<ChunkPayload> bounded = new ArrayList<>();
        for (ChunkPayload chunk : chunks) {
            bounded.addAll(splitOversizedChunk(
                chunk
            ));
        }

        log.debug(
            "StructureAwareChunker({}) Outcome(articleId:{},rawChunkCount:{},boundedChunkCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            article.getId(),
            chunks.size(),
            bounded.size()
        );

        return bounded;
    }

    private void addTitleChunk(
        List<ChunkPayload> chunks,
        String title
    ) {
        String normalizedTitle = StringUtils.trimToNull(title);
        if (Objects.isNull(normalizedTitle)) {
            return;
        }

        chunks.add(
            ChunkPayload.builder()
                        .chunkType(KnowledgeChunkType.TITLE)
                        .sectionPath("title")
                        .content(normalizedTitle)
                        .tokenCount(openNlpTokenizer.tokenCount(
                            normalizedTitle
                        ))
                        .build()
        );
    }

    private void processMarkdownNode(
        List<ChunkPayload> chunks,
        Node node,
        String currentSectionPath
    ) {
        String sectionPath = currentSectionPath;

        if (node instanceof Heading heading) {
            String headerText = extractText(heading);
            sectionPath = normalizeSectionPath(headerText);

            addAtomicChunk(
                chunks,
                KnowledgeChunkType.SECTION_HEADER,
                sectionPath,
                headerText
            );
        } else if (node instanceof Paragraph paragraph) {
            String text = extractText(paragraph);

            switch (paragraphChunkType(text)) {
                case FAQ_Q -> addAtomicChunk(
                    chunks,
                    KnowledgeChunkType.FAQ_Q,
                    sectionPath,
                    text
                );
                case FAQ_A -> addAtomicChunk(
                    chunks,
                    KnowledgeChunkType.FAQ_A,
                    sectionPath,
                    text
                );
                case BODY -> addAtomicChunk(
                    chunks,
                    KnowledgeChunkType.BODY,
                    sectionPath,
                    text
                );
                case NONE -> {
                    // no-op
                }
            }
        }

        Node child = node.getFirstChild();
        while (child != null) {
            processMarkdownNode(
                chunks,
                child,
                sectionPath
            );
            child = child.getNext();
        }
    }

    private ParagraphChunkType paragraphChunkType(
        String text
    ) {
        if (StringUtils.isBlank(text)) {
            return ParagraphChunkType.NONE;
        }

        if (startsWithIgnoreCase(
            text,
            "Q:"
        )) {
            return ParagraphChunkType.FAQ_Q;
        }

        if (startsWithIgnoreCase(
            text,
            "A:"
        )) {
            return ParagraphChunkType.FAQ_A;
        }

        return ParagraphChunkType.BODY;
    }

    private boolean startsWithIgnoreCase(
        String text,
        String prefix
    ) {
        if (Objects.isNull(text) || Objects.isNull(prefix)) {
            return false;
        }

        if (text.length() < prefix.length()) {
            return false;
        }

        return text.regionMatches(
            true,
            0,
            prefix,
            0,
            prefix.length()
        );
    }

    private String extractText(
        Node node
    ) {
        StringBuilder text = new StringBuilder();

        if (node instanceof Text textNode) {
            text.append(textNode.getLiteral());
        }

        Node child = node.getFirstChild();
        while (child != null) {
            text.append(extractText(child));
            child = child.getNext();
        }

        return text.toString().trim();
    }

    private void addAtomicChunk(
        List<ChunkPayload> chunks,
        KnowledgeChunkType chunkType,
        String sectionPath,
        String content
    ) {
        chunks.add(
            ChunkPayload.builder()
                        .chunkType(chunkType)
                        .sectionPath(sectionPath)
                        .content(content)
                        .tokenCount(openNlpTokenizer.tokenCount(
                            content
                        ))
                        .build()
        );
    }

    private List<ChunkPayload> splitOversizedChunk(
        ChunkPayload chunk
    ) {
        if (chunk.getTokenCount() <= ragConfiguration.getChunkMaxTokens()) {
            return List.of(chunk);
        }

        List<String> sentences = openNlpTokenizer.detectSentences(
            chunk.getContent()
        );

        List<ChunkPayload> splitChunks = buildSentenceBasedChunks(
            chunk,
            sentences
        );

        log.debug(
            "StructureAwareChunker({}) Outcome(chunkType:{},sectionPath:{},inputTokens:{},outputChunks:{})",
            OperationalLogContext.PHASE_DECISION,
            chunk.getChunkType(),
            chunk.getSectionPath(),
            chunk.getTokenCount(),
            splitChunks.size()
        );

        return splitChunks;
    }

    private List<ChunkPayload> buildSentenceBasedChunks(
        ChunkPayload originalChunk,
        List<String> sentences
    ) {
        List<ChunkPayload> result = new ArrayList<>();

        int maxTokens = ragConfiguration.getChunkMaxTokens();
        int overlapTokens = Math.clamp(
            ragConfiguration.getChunkOverlapTokens(),
            0,
            Math.max(0, maxTokens - 1)
        );

        List<String> currentChunkSentences = new ArrayList<>();
        int currentTokenCount = 0;

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            int sentenceTokens = openNlpTokenizer.tokenCount(
                sentence
            );

            if (sentenceTokens > maxTokens) {
                if (!currentChunkSentences.isEmpty()) {
                    result.add(buildChunk(
                        originalChunk,
                        currentChunkSentences
                    ));
                    currentChunkSentences.clear();
                    currentTokenCount = 0;
                }

                result.addAll(splitLongSentence(
                    originalChunk,
                    sentence,
                    maxTokens,
                    overlapTokens
                ));
                continue;
            }

            if (currentTokenCount + sentenceTokens > maxTokens && !currentChunkSentences.isEmpty()) {
                result.add(buildChunk(
                    originalChunk,
                    currentChunkSentences
                ));

                currentChunkSentences.clear();
                currentTokenCount = 0;

                int overlapSentences = calculateOverlapSentences(
                    sentences,
                    i,
                    overlapTokens
                );

                for (int j = Math.max(0, i - overlapSentences); j < i; j++) {
                    currentChunkSentences.add(sentences.get(j));
                    currentTokenCount += openNlpTokenizer.tokenCount(
                        sentences.get(j)
                    );
                }
            }

            currentChunkSentences.add(sentence);
            currentTokenCount += sentenceTokens;
        }

        if (!currentChunkSentences.isEmpty()) {
            result.add(buildChunk(
                originalChunk,
                currentChunkSentences
            ));
        }

        return result.isEmpty() ? List.of(originalChunk) : result;
    }

    private int calculateOverlapSentences(
        List<String> sentences,
        int currentIndex,
        int targetOverlapTokens
    ) {
        int overlapTokenCount = 0;
        int overlapSentences = 0;

        for (int i = currentIndex - 1; i >= 0 && overlapTokenCount < targetOverlapTokens; i--) {
            overlapTokenCount += openNlpTokenizer.tokenCount(
                sentences.get(i)
            );
            overlapSentences++;
        }

        return overlapSentences;
    }

    private List<ChunkPayload> splitLongSentence(
        ChunkPayload originalChunk,
        String sentence,
        int maxTokens,
        int overlapTokens
    ) {
        List<String> tokens = openNlpTokenizer.tokenize(
            sentence
        );

        List<ChunkPayload> result = new ArrayList<>();

        int cursor = 0;

        while (cursor < tokens.size()) {
            int endExclusive = Math.min(
                tokens.size(),
                cursor + maxTokens
            );

            String content = String.join(
                StringUtils.SPACE,
                tokens.subList(
                    cursor,
                    endExclusive
                )
            );

            result.add(
                ChunkPayload.builder()
                            .chunkType(originalChunk.getChunkType())
                            .sectionPath(originalChunk.getSectionPath())
                            .content(content)
                            .tokenCount(openNlpTokenizer.tokenCount(
                                content
                            ))
                            .build()
            );

            if (endExclusive >= tokens.size()) {
                break;
            }

            cursor = Math.max(
                cursor + 1,
                endExclusive - overlapTokens
            );
        }

        return result;
    }

    private ChunkPayload buildChunk(
        ChunkPayload originalChunk,
        List<String> sentences
    ) {
        String content = String.join(
            StringUtils.SPACE,
            sentences
        );

        return ChunkPayload.builder()
                           .chunkType(originalChunk.getChunkType())
                           .sectionPath(originalChunk.getSectionPath())
                           .content(content)
                           .tokenCount(openNlpTokenizer.tokenCount(
                               content
                           ))
                           .build();
    }

    private String normalizeSectionPath(
        String headerText
    ) {
        String normalized = StringUtils.trimToEmpty(headerText);

        String slugified = slugify.slugify(normalized);

        return StringUtils.defaultIfBlank(
            slugified,
            "content"
        );
    }

    private enum ParagraphChunkType {
        NONE,
        FAQ_Q,
        FAQ_A,
        BODY
    }

    @Getter
    @lombok.Builder
    public static class ChunkPayload {
        private KnowledgeChunkType chunkType;
        private String sectionPath;
        private String content;
        private Integer tokenCount;
    }
}
