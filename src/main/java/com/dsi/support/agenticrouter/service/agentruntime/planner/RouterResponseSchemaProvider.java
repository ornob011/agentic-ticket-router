package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.service.ai.PromptService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RouterResponseSchemaProvider {

    private static final String PROPERTIES = "properties";
    private static final String ENUM = "enum";
    private static final String CATEGORY = "category";
    private static final String PRIORITY = "priority";
    private static final String QUEUE = "queue";
    private static final String NEXT_ACTION = "next_action";

    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    private JsonNode baseSchema;

    @PostConstruct
    public void initialize() throws IOException {
        String schemaText = promptService.getRouterResponseSchema()
                                         .getContentAsString(StandardCharsets.UTF_8);

        this.baseSchema = objectMapper.readTree(schemaText);
    }

    public JsonNode runtimeSchemaJson() {
        ObjectNode schema = baseSchema.deepCopy();
        ObjectNode propertiesNode = (ObjectNode) schema.get(PROPERTIES);

        withEnumValues(
            propertiesNode,
            CATEGORY,
            enumNames(TicketCategory.values())
        );
        withEnumValues(
            propertiesNode,
            PRIORITY,
            enumNames(TicketPriority.values())
        );
        withEnumValues(
            propertiesNode,
            QUEUE,
            enumNames(TicketQueue.values())
        );
        withEnumValues(
            propertiesNode,
            NEXT_ACTION,
            enumNames(NextAction.values())
        );

        return schema;
    }

    public Map<String, Object> runtimeSchemaMap() {
        return objectMapper.convertValue(
            runtimeSchemaJson(),
            new TypeReference<>() {
            }
        );
    }

    private void withEnumValues(
        ObjectNode propertiesNode,
        String field,
        List<String> values
    ) {
        JsonNode fieldNode = propertiesNode.get(field);
        if (!(fieldNode instanceof ObjectNode objectNode)) {
            return;
        }

        ArrayNode enumNode = objectNode.putArray(ENUM);
        values.forEach(enumNode::add);
    }

    private List<String> enumNames(
        Enum<?>[] values
    ) {
        return Arrays.stream(values)
                     .map(Enum::name)
                     .toList();
    }
}
