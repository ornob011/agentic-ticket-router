package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.GlobalConfig;
import com.dsi.support.agenticrouter.enums.GlobalConfigKey;
import com.dsi.support.agenticrouter.enums.VectorStoreConfigKey;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.GlobalConfigRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreInitializationService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final GlobalConfigRepository globalConfigRepository;
    private final TransactionTemplate transactionTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeVectorStore() {
        log.info(
            "VectorStoreInitialization({})",
            OperationalLogContext.PHASE_START
        );

        Boolean isInitialized = transactionTemplate.execute(status -> {
            GlobalConfig globalConfig = globalConfigRepository.findByConfigKey(
                GlobalConfigKey.VECTOR_STORE_INITIALIZED
            ).orElse(
                GlobalConfig.builder()
                            .configKey(GlobalConfigKey.VECTOR_STORE_INITIALIZED)
                            .build()
            );

            Boolean value = globalConfig.getValue(
                VectorStoreConfigKey.INITIALIZED.name(),
                Boolean.class
            );

            return Boolean.TRUE.equals(value);
        });

        if (Boolean.TRUE.equals(isInitialized)) {
            log.info(
                "VectorStoreInitialization({}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                "already_initialized"
            );

            return;
        }

        log.info(
            "VectorStoreInitialization({}) Outcome(action:{})",
            OperationalLogContext.PHASE_DECISION,
            "run_initialization"
        );

        knowledgeBaseService.initializeVectorStore();

        transactionTemplate.executeWithoutResult(status -> {
            GlobalConfig globalConfig = globalConfigRepository.findByConfigKey(
                GlobalConfigKey.VECTOR_STORE_INITIALIZED
            ).orElse(
                GlobalConfig.builder()
                            .configKey(GlobalConfigKey.VECTOR_STORE_INITIALIZED)
                            .build()
            );

            globalConfig.setValue(
                VectorStoreConfigKey.INITIALIZED.name(),
                true
            );

            globalConfig.setUpdatedAt(Instant.now());

            globalConfigRepository.save(globalConfig);
        });

        log.info(
            "VectorStoreInitialization({}) Outcome(configKey:{},initialized:{})",
            OperationalLogContext.PHASE_COMPLETE,
            GlobalConfigKey.VECTOR_STORE_INITIALIZED,
            true
        );
    }

    public void forceReinitialize() {
        log.warn(
            "VectorStoreReinitialize({}) Outcome(action:{})",
            OperationalLogContext.PHASE_START,
            "force_reinitialize"
        );

        transactionTemplate.executeWithoutResult(status -> {
            GlobalConfig globalConfig = globalConfigRepository.findByConfigKey(
                GlobalConfigKey.VECTOR_STORE_INITIALIZED
            ).orElseThrow(
                DataNotFoundException.supplier(
                    GlobalConfig.class,
                    GlobalConfigKey.VECTOR_STORE_INITIALIZED
                )
            );

            globalConfig.setValue(
                VectorStoreConfigKey.INITIALIZED.name(),
                false
            );

            globalConfig.setUpdatedAt(Instant.now());

            globalConfigRepository.save(globalConfig);
        });

        knowledgeBaseService.initializeVectorStore();

        transactionTemplate.executeWithoutResult(status -> {
            GlobalConfig globalConfig = globalConfigRepository.findByConfigKey(
                GlobalConfigKey.VECTOR_STORE_INITIALIZED
            ).orElseThrow(
                DataNotFoundException.supplier(
                    GlobalConfig.class,
                    GlobalConfigKey.VECTOR_STORE_INITIALIZED
                )
            );

            globalConfig.setValue(
                VectorStoreConfigKey.INITIALIZED.name(),
                true
            );

            globalConfig.setUpdatedAt(Instant.now());

            globalConfigRepository.save(globalConfig);
        });

        log.info(
            "VectorStoreReinitialize({}) Outcome(configKey:{},initialized:{})",
            OperationalLogContext.PHASE_COMPLETE,
            GlobalConfigKey.VECTOR_STORE_INITIALIZED,
            true
        );
    }
}
