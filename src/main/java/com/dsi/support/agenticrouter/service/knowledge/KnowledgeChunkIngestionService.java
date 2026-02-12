package com.dsi.support.agenticrouter.service.knowledge;

import com.dsi.support.agenticrouter.configuration.VectorStoreIngestionConfiguration;
import com.dsi.support.agenticrouter.entity.KnowledgeArticle;
import com.dsi.support.agenticrouter.enums.VectorStoreMetadataKey;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.DelegatingProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeChunkIngestionService {

    private final KnowledgeBaseVectorStore knowledgeBaseVectorStore;
    private final StructureAwareChunker structureAwareChunker;
    private final VectorStoreIngestionConfiguration vectorStoreIngestionConfiguration;

    public void build(
        List<KnowledgeArticle> knowledgeArticles
    ) {
        log.info(
            "KnowledgeChunkBuild({}) Outcome(articleCount:{})",
            OperationalLogContext.PHASE_START,
            knowledgeArticles.size()
        );

        knowledgeBaseVectorStore.removeAll();
        syncIncremental(knowledgeArticles);

        log.info(
            "KnowledgeChunkBuild({}) Outcome(status:{})",
            OperationalLogContext.PHASE_COMPLETE,
            "success"
        );
    }

    public void syncIncremental(
        List<KnowledgeArticle> knowledgeArticles
    ) {
        if (knowledgeArticles.isEmpty()) {
            log.debug(
                "KnowledgeChunkBuild({}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                "no_articles"
            );
            return;
        }

        ProgressBarBuilder progressBarBuilder = new ProgressBarBuilder().setTaskName("Sync articles")
                                                                        .setInitialMax(knowledgeArticles.size())
                                                                        .setUpdateIntervalMillis(vectorStoreIngestionConfiguration.getProgressUpdateIntervalMs())
                                                                        .setStyle(ProgressBarStyle.ASCII)
                                                                        .setConsumer(new DelegatingProgressBarConsumer(log::info));

        List<Document> vectorBatch = new ArrayList<>(vectorStoreIngestionConfiguration.getIngestBatchSize());

        int processedArticles = 0;
        int skippedArticles = 0;
        int chunkDocumentCount = 0;

        long startTimeMillis = System.currentTimeMillis();

        for (KnowledgeArticle knowledgeArticle : ProgressBar.wrap(knowledgeArticles, progressBarBuilder)) {
            if (Objects.isNull(knowledgeArticle.getId()) || !Boolean.TRUE.equals(knowledgeArticle.getActive())) {
                skippedArticles++;
                continue;
            }

            List<Document> articleDocuments = toDocuments(knowledgeArticle);
            if (articleDocuments.isEmpty()) {
                skippedArticles++;
                continue;
            }

            knowledgeBaseVectorStore.deleteByArticleId(knowledgeArticle.getId());

            vectorBatch.addAll(articleDocuments);
            chunkDocumentCount += articleDocuments.size();
            processedArticles++;

            if (vectorBatch.size() >= vectorStoreIngestionConfiguration.getIngestBatchSize()) {
                flushBatch(vectorBatch);
            }
        }

        flushBatch(vectorBatch);

        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;

        log.info(
            "KnowledgeChunkBuild({}) Outcome(processedArticles:{},skippedArticles:{},chunkDocumentCount:{},elapsedMs:{})",
            OperationalLogContext.PHASE_COMPLETE,
            processedArticles,
            skippedArticles,
            chunkDocumentCount,
            elapsedMillis
        );
    }

    private void flushBatch(
        List<Document> vectorBatch
    ) {
        if (vectorBatch.isEmpty()) {
            return;
        }

        log.debug(
            "KnowledgeChunkBuild({}) Outcome(action:{},batchSize:{})",
            OperationalLogContext.PHASE_DECISION,
            "vector_add",
            vectorBatch.size()
        );

        knowledgeBaseVectorStore.addDocuments(new ArrayList<>(vectorBatch));
        vectorBatch.clear();
    }

    private List<Document> toDocuments(
        KnowledgeArticle knowledgeArticle
    ) {
        List<StructureAwareChunker.ChunkPayload> payloads = structureAwareChunker.chunkArticle(knowledgeArticle);

        List<Document> documents = new ArrayList<>(payloads.size());

        for (int chunkIndex = 0; chunkIndex < payloads.size(); chunkIndex++) {
            StructureAwareChunker.ChunkPayload payload = payloads.get(chunkIndex);

            String contentHash = DigestUtils.sha256Hex(payload.getContent().getBytes(StandardCharsets.UTF_8));
            String documentId = UUID.nameUUIDFromBytes(
                String.format(
                    "kb_chunk_%d_%d_%s",
                    knowledgeArticle.getId(),
                    chunkIndex,
                    contentHash
                ).getBytes(StandardCharsets.UTF_8)
            ).toString();

            documents.add(
                new Document(
                    documentId,
                    payload.getContent(),
                    buildMetadata(
                        knowledgeArticle,
                        payload,
                        chunkIndex,
                        contentHash
                    )
                )
            );
        }

        return documents;
    }

    private Map<String, Object> buildMetadata(
        KnowledgeArticle knowledgeArticle,
        StructureAwareChunker.ChunkPayload chunkPayload,
        int chunkIndex,
        String contentHash
    ) {
        Map<String, Object> metadata = HashMap.newHashMap(
            16
        );

        metadata.put(
            VectorStoreMetadataKey.ARTICLE_ID.name(),
            knowledgeArticle.getId()
        );

        metadata.put(
            VectorStoreMetadataKey.CATEGORY.name(),
            knowledgeArticle.getCategory().name()
        );

        metadata.put(
            VectorStoreMetadataKey.PRIORITY.name(),
            knowledgeArticle.getPriority()
        );

        metadata.put(
            VectorStoreMetadataKey.ARTICLE_TYPE.name(),
            knowledgeArticle.getArticleType().name()
        );

        metadata.put(
            VectorStoreMetadataKey.TITLE.name(),
            knowledgeArticle.getTitle()
        );

        metadata.put(
            VectorStoreMetadataKey.ACTIVE.name(),
            knowledgeArticle.getActive()
        );

        metadata.put(
            VectorStoreMetadataKey.CHUNK_INDEX.name(),
            chunkIndex
        );

        metadata.put(
            VectorStoreMetadataKey.CHUNK_TYPE.name(),
            chunkPayload.getChunkType().name()
        );

        metadata.put(
            VectorStoreMetadataKey.SECTION_PATH.name(),
            StringUtils.defaultString(
                chunkPayload.getSectionPath()
            )
        );

        metadata.put(
            VectorStoreMetadataKey.CONTENT_HASH.name(),
            contentHash
        );

        return metadata;
    }

}
