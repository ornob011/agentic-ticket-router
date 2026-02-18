package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.AgentValidationErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidatorTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AgentPlanSchemaValidator {

    private static final JacksonJsonParser JSON_PARSER = new JacksonJsonParser();

    private static final Map<String, AgentValidationErrorCode> ERROR_CODE_BY_VALIDATOR_TYPE = Map.of(
        ValidatorTypeCode.REQUIRED.getValue(),
        AgentValidationErrorCode.MISSING_FIELD,
        ValidatorTypeCode.TYPE.getValue(),
        AgentValidationErrorCode.INVALID_TYPE,
        ValidatorTypeCode.ENUM.getValue(),
        AgentValidationErrorCode.INVALID_ENUM
    );

    private static final Set<String> OUT_OF_RANGE_VALIDATOR_TYPES = Set.of(
        ValidatorTypeCode.MINIMUM.getValue(),
        ValidatorTypeCode.MAXIMUM.getValue(),
        ValidatorTypeCode.EXCLUSIVE_MINIMUM.getValue(),
        ValidatorTypeCode.EXCLUSIVE_MAXIMUM.getValue()
    );

    private final ObjectMapper objectMapper;
    private final RouterResponseSchemaProvider routerResponseSchemaProvider;

    public AgentPlanValidationResult validate(
        String rawPlannerJson
    ) {
        JsonNode responseJson = parseJsonNode(
            rawPlannerJson
        );

        JsonSchema schema = JsonSchemaFactory.getInstance(
            SpecVersion.VersionFlag.V202012
        ).getSchema(
            routerResponseSchemaProvider.runtimeSchemaJson()
        );

        Set<ValidationMessage> validationMessages = schema.validate(
            responseJson
        );

        if (validationMessages.isEmpty()) {
            return AgentPlanValidationResult.builder()
                                            .valid(true)
                                            .errorCode(null)
                                            .errorMessage(null)
                                            .jsonNode(responseJson)
                                            .build();
        }

        ValidationMessage firstValidationMessage = validationMessages.iterator().next();

        return AgentPlanValidationResult.builder()
                                        .valid(false)
                                        .errorCode(errorCodeFor(firstValidationMessage))
                                        .errorMessage(firstValidationMessage.getMessage())
                                        .jsonNode(responseJson)
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
        String rawPlannerJson
    ) {
        String normalizedPlannerJson = Objects.requireNonNullElse(
            rawPlannerJson,
            "{}"
        );

        Object parsedJsonObject = JSON_PARSER.parseMap(normalizedPlannerJson);

        return objectMapper.valueToTree(parsedJsonObject);
    }

    private AgentValidationErrorCode errorCodeFor(
        ValidationMessage validationMessage
    ) {
        String validatorType = validationMessage.getType();

        if (OUT_OF_RANGE_VALIDATOR_TYPES.contains(validatorType)) {
            return AgentValidationErrorCode.OUT_OF_RANGE;
        }

        return ERROR_CODE_BY_VALIDATOR_TYPE.getOrDefault(
            validatorType,
            AgentValidationErrorCode.INVALID_JSON
        );
    }
}
