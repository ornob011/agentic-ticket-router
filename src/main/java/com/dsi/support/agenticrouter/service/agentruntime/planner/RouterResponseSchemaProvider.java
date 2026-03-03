package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.service.ai.PromptService;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RouterResponseSchemaProvider {

    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    private RouterResponseSchema baseSchema;

    public RouterResponseSchemaProvider(
        ObjectMapper objectMapper,
        PromptService promptService
    ) {
        this.objectMapper = objectMapper;
        this.promptService = promptService;
    }

    @PostConstruct
    public void initialize() throws IOException {
        String schemaText = promptService.getRouterResponseSchema()
                                         .getContentAsString(StandardCharsets.UTF_8);

        this.baseSchema = objectMapper.readValue(
            schemaText,
            RouterResponseSchema.class
        );
    }

    public JsonNode runtimeSchemaJson() {
        RouterResponseSchema runtimeSchema = objectMapper.convertValue(
            baseSchema,
            RouterResponseSchema.class
        );

        RouterResponseSchemaProperties properties = runtimeSchema.getProperties();

        properties.getCategory().setEnumValues(enumNames(TicketCategory.values()));
        properties.getPriority().setEnumValues(enumNames(TicketPriority.values()));
        properties.getQueue().setEnumValues(enumNames(TicketQueue.values()));
        properties.getNextAction().setEnumValues(enumNames(NextAction.values()));

        return objectMapper.valueToTree(runtimeSchema);
    }

    public Map<String, Object> runtimeSchemaMap() {
        return objectMapper.convertValue(
            runtimeSchemaJson(),
            new TypeReference<>() {
            }
        );
    }

    private List<String> enumNames(
        Enum<?>[] values
    ) {
        return Arrays.stream(values)
                     .map(Enum::name)
                     .toList();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RouterResponseSchema {

        @JsonProperty("properties")
        private RouterResponseSchemaProperties properties = new RouterResponseSchemaProperties();

        private final Map<String, Object> additional = new LinkedHashMap<>();

        @JsonAnySetter
        void setAdditional(
            String key,
            Object value
        ) {
            additional.put(key, value);
        }

        @JsonAnyGetter
        Map<String, Object> getAdditional() {
            return additional;
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RouterResponseSchemaProperties {

        @JsonProperty("category")
        private SchemaField category = new SchemaField();

        @JsonProperty("priority")
        private SchemaField priority = new SchemaField();

        @JsonProperty("queue")
        private SchemaField queue = new SchemaField();

        @JsonProperty("next_action")
        private SchemaField nextAction = new SchemaField();

        private final Map<String, Object> additional = new LinkedHashMap<>();

        @JsonAnySetter
        void setAdditional(
            String key,
            Object value
        ) {
            additional.put(key, value);
        }

        @JsonAnyGetter
        Map<String, Object> getAdditional() {
            return additional;
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SchemaField {

        @JsonProperty("enum")
        private List<String> enumValues;

        private final Map<String, Object> additional = new LinkedHashMap<>();

        @JsonAnySetter
        void setAdditional(
            String key,
            Object value
        ) {
            additional.put(key, value);
        }

        @JsonAnyGetter
        Map<String, Object> getAdditional() {
            return additional;
        }
    }
}
