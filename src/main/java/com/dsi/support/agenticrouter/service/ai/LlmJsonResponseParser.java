package com.dsi.support.agenticrouter.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.converter.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class LlmJsonResponseParser {

    private static final ResponseTextCleaner JSON_RESPONSE_CLEANER = CompositeResponseTextCleaner.builder()
                                                                                                 .addCleaner(new MarkdownCodeBlockCleaner())
                                                                                                 .addCleaner(new ThinkingTagCleaner())
                                                                                                 .build();

    private final ObjectMapper objectMapper;

    public JsonNode parseJsonObjectToNode(
        String rawResponse
    ) {
        Map<String, Object> parsedJson = parseJsonObjectToMap(
            rawResponse
        );

        return objectMapper.valueToTree(
            parsedJson
        );
    }

    public Map<String, Object> parseJsonObjectToMap(
        String rawResponse
    ) {
        String normalizedResponse = Objects.requireNonNullElse(
            rawResponse,
            "{}"
        );

        BeanOutputConverter<Map<String, Object>> outputConverter = new BeanOutputConverter<>(
            new ParameterizedTypeReference<>() {
            },
            objectMapper,
            JSON_RESPONSE_CLEANER
        );

        return outputConverter.convert(
            normalizedResponse
        );
    }
}
