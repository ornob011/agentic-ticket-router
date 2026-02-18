package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.AgentValidationErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AgentPlanSchemaValidator {

    private static final JacksonJsonParser JSON_PARSER = new JacksonJsonParser();

    private final ObjectMapper objectMapper;
    private final RouterResponseSchemaProvider routerResponseSchemaProvider;

    public AgentPlanValidationResult validate(
        String rawJson
    ) {
        JsonNode jsonNode = parseJsonNode(
            rawJson
        );
        JsonSchema schema = JsonSchemaFactory.getInstance(
            SpecVersion.VersionFlag.V202012
        ).getSchema(routerResponseSchemaProvider.runtimeSchemaJson());
        Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

        if (validationMessages.isEmpty()) {
            return AgentPlanValidationResult.builder()
                                            .valid(true)
                                            .errorCode(null)
                                            .errorMessage(null)
                                            .jsonNode(jsonNode)
                                            .build();
        }

        ValidationMessage firstMessage = validationMessages.iterator().next();
        return AgentPlanValidationResult.builder()
                                        .valid(false)
                                        .errorCode(errorCodeFor(firstMessage))
                                        .errorMessage(firstMessage.getMessage())
                                        .jsonNode(jsonNode)
                                        .build();
    }

    public RouterResponse map(
        JsonNode jsonNode
    ) {
        return objectMapper.convertValue(
            jsonNode,
            RouterResponse.class
        );
    }

    private JsonNode parseJsonNode(
        String rawJson
    ) {
        String normalizedJson = Objects.requireNonNullElse(
            rawJson,
            "{}"
        );
        Object parsed = JSON_PARSER.parseMap(normalizedJson);
        return objectMapper.valueToTree(parsed);
    }

    private AgentValidationErrorCode errorCodeFor(
        ValidationMessage validationMessage
    ) {
        String type = validationMessage.getType();
        if (Objects.equals(type, "required")) {
            return AgentValidationErrorCode.MISSING_FIELD;
        }

        if (Objects.equals(type, "type")) {
            return AgentValidationErrorCode.INVALID_TYPE;
        }

        if (Objects.equals(type, "enum")) {
            return AgentValidationErrorCode.INVALID_ENUM;
        }

        if (Objects.equals(type, "minimum")
            || Objects.equals(type, "maximum")
            || Objects.equals(type, "exclusiveMinimum")
            || Objects.equals(type, "exclusiveMaximum")
        ) {
            return AgentValidationErrorCode.OUT_OF_RANGE;
        }

        return AgentValidationErrorCode.INVALID_JSON;
    }
}
