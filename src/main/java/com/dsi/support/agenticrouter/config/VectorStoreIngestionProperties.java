package com.dsi.support.agenticrouter.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "agenticrouter.vectorstore.ingestion")
public class VectorStoreIngestionProperties {

    @Min(1)
    private int ingestBatchSize = 200;

    @Min(1)
    private int splitChunkSize = 1200;

    @Min(1)
    private int splitMinChunkSizeChars = 200;

    @Min(0)
    private int splitMinChunkLengthToEmbed = 30;

    @Min(100)
    private int progressUpdateIntervalMs = 1000;
}
