package com.dsi.support.agenticrouter.service.ai;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.converter.CompositeResponseTextCleaner;
import org.springframework.ai.converter.MarkdownCodeBlockCleaner;
import org.springframework.ai.converter.ResponseTextCleaner;
import org.springframework.ai.converter.ThinkingTagCleaner;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class LlmResponseTextExtractor {

    private static final ResponseTextCleaner RESPONSE_TEXT_CLEANER = CompositeResponseTextCleaner.builder()
                                                                                                 .addCleaner(new MarkdownCodeBlockCleaner())
                                                                                                 .addCleaner(new ThinkingTagCleaner())
                                                                                                 .build();

    public String extractRequiredContent(
        ChatClient.CallResponseSpec callResponseSpec,
        String operation
    ) {
        ChatResponse chatResponse = callResponseSpec.chatResponse();
        Objects.requireNonNull(chatResponse, "llm.chatResponse");

        Generation generation = Objects.requireNonNull(
            chatResponse.getResult(),
            "llm.generation"
        );

        AssistantMessage assistantMessage = Objects.requireNonNull(
            generation.getOutput(),
            "llm.assistantMessage"
        );

        String rawResponseText = StringUtils.defaultString(
            assistantMessage.getText()
        );

        String responseText = RESPONSE_TEXT_CLEANER.clean(
            rawResponseText
        );

        if (StringUtils.isNotBlank(responseText)) {
            return responseText;
        }

        if (StringUtils.isNotBlank(rawResponseText)) {
            return rawResponseText;
        }

        ChatGenerationMetadata generationMetadata = generation.getMetadata();

        Object thinkingValue = generationMetadata.get("thinking");

        String finishReason = generationMetadata.getFinishReason();

        String model = chatResponse.getMetadata()
                                   .getModel();

        throw new IllegalStateException(
            String.format(
                "LLM response content is empty for operation=%s, model=%s, finishReason=%s, hasThinking=%s",
                operation,
                StringUtils.defaultIfBlank(model, "unknown"),
                StringUtils.defaultIfBlank(finishReason, "unknown"),
                StringUtils.isNotBlank(StringUtils.trimToNull(Objects.toString(thinkingValue, null)))
            )
        );
    }
}
