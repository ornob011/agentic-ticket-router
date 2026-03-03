package com.dsi.support.agenticrouter.configuration;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;
import org.springframework.ai.model.ollama.autoconfigure.OllamaInitializationProperties;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "ollama")
public class DraftReplyOllamaConfiguration {

    @Bean
    @Primary
    public ChatModel primaryChatModel(
        @Qualifier("ollamaChatModel") ChatModel ollamaChatModel
    ) {
        return ollamaChatModel;
    }

    @Bean(name = "draftReplyChatModel")
    public ChatModel draftReplyChatModel(
        OllamaApi ollamaApi,
        OllamaChatProperties ollamaChatProperties,
        OllamaInitializationProperties ollamaInitializationProperties,
        ToolCallingManager toolCallingManager,
        ObjectProvider<ObservationRegistry> observationRegistryProvider,
        ObjectProvider<ChatModelObservationConvention> chatModelObservationConventionProvider,
        ObjectProvider<ToolExecutionEligibilityPredicate> toolExecutionEligibilityPredicateProvider,
        RetryTemplate retryTemplate
    ) {
        PullModelStrategy pullModelStrategy = ollamaInitializationProperties.getChat().isInclude()
            ? ollamaInitializationProperties.getPullModelStrategy()
            : PullModelStrategy.NEVER;

        OllamaChatOptions draftOptions = OllamaChatOptions.fromOptions(
            ollamaChatProperties.getOptions()
        );
        draftOptions.setFormat(null);

        OllamaChatModel draftModel = OllamaChatModel.builder()
                                                    .ollamaApi(ollamaApi)
                                                    .defaultOptions(draftOptions)
                                                    .toolCallingManager(toolCallingManager)
                                                    .toolExecutionEligibilityPredicate(
                                                        toolExecutionEligibilityPredicateProvider.getIfUnique(
                                                            DefaultToolExecutionEligibilityPredicate::new
                                                        )
                                                    )
                                                    .observationRegistry(
                                                        observationRegistryProvider.getIfUnique(
                                                            ObservationRegistry::create
                                                        )
                                                    )
                                                    .modelManagementOptions(
                                                        new ModelManagementOptions(
                                                            pullModelStrategy,
                                                            ollamaInitializationProperties.getChat()
                                                                                          .getAdditionalModels(),
                                                            ollamaInitializationProperties.getTimeout(),
                                                            ollamaInitializationProperties.getMaxRetries()
                                                        )
                                                    )
                                                    .retryTemplate(retryTemplate)
                                                    .build();

        chatModelObservationConventionProvider.ifAvailable(
            draftModel::setObservationConvention
        );

        return draftModel;
    }
}
