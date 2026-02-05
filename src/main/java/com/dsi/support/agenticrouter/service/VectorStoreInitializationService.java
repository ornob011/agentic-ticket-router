package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.GlobalConfig;
import com.dsi.support.agenticrouter.enums.GlobalConfigKey;
import com.dsi.support.agenticrouter.enums.VectorStoreConfigKey;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.GlobalConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreInitializationService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final GlobalConfigRepository globalConfigRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeVectorStore() {
        log.info("VectorStore initialization check triggered on application startup");

        GlobalConfig globalConfig = globalConfigRepository.findByConfigKeyAndActiveTrue(
            GlobalConfigKey.VECTOR_STORE_INITIALIZED
        ).orElse(
            GlobalConfig.builder()
                        .configKey(GlobalConfigKey.VECTOR_STORE_INITIALIZED)
                        .build()
        );

        boolean isInitialized = globalConfig.getValue(
            VectorStoreConfigKey.INITIALIZED.name(),
            Boolean.class
        );

        if (isInitialized) {
            log.info("VectorStore already initialized, skipping");
            return;
        }

        log.info("VectorStore not initialized, starting one-time initialization");

        knowledgeBaseService.initializeVectorStore();

        globalConfig.setValue(
            VectorStoreConfigKey.INITIALIZED.name(),
            true
        );

        globalConfig.setUpdatedAt(Instant.now());

        globalConfigRepository.save(globalConfig);
    }

    @Transactional
    public void forceReinitialize() {
        log.warn("Forcing vector store re-initialization");

        GlobalConfig config = globalConfigRepository.findByConfigKeyAndActiveTrue(
            GlobalConfigKey.VECTOR_STORE_INITIALIZED
        ).orElseThrow(
            DataNotFoundException.supplier(
                GlobalConfig.class,
                GlobalConfigKey.VECTOR_STORE_INITIALIZED
            )
        );

        config.setValue(
            VectorStoreConfigKey.INITIALIZED.name(),
            false
        );

        globalConfigRepository.save(config);

        knowledgeBaseService.initializeVectorStore();

        config.setValue(
            VectorStoreConfigKey.INITIALIZED.name(),
            true
        );
        config.setUpdatedAt(Instant.now());

        globalConfigRepository.save(config);

        log.info("VectorStore force re-initialization completed");
    }
}
