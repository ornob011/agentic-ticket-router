package com.dsi.support.agenticrouter.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Objects;

@Converter
public class TicketAutonomousMetadataConverter implements AttributeConverter<TicketAutonomousMetadata, JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JsonNode convertToDatabaseColumn(
        TicketAutonomousMetadata autonomousMetadata
    ) {
        if (Objects.isNull(autonomousMetadata)) {
            return null;
        }

        return objectMapper.valueToTree(autonomousMetadata);
    }

    @Override
    public TicketAutonomousMetadata convertToEntityAttribute(
        JsonNode jsonNode
    ) {
        if (Objects.isNull(jsonNode)) {
            return null;
        }

        try {
            return objectMapper.treeToValue(jsonNode, TicketAutonomousMetadata.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to convert JSON to TicketAutonomousMetadata", exception);
        }
    }
}
