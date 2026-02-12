package com.dsi.support.agenticrouter.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
@ConfigurationProperties(prefix = "agenticrouter.rag")
public class RagConfiguration {

    private boolean enabled = false;

    private boolean shadowEnabled = true;

    @Min(1)
    private int returnTopK = 5;

    @Min(1)
    private int maxRounds = 2;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double minConfidence = 0.45D;

    @Min(1)
    private int chunkMaxTokens = 450;

    @Min(0)
    private int chunkOverlapTokens = 70;

    @Min(1)
    private int denseTopN = 80;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double similarityThreshold = 0.82D;

    @Min(1)
    private int lexicalTopM = 50;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double denseWeight = 0.55D;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double lexicalWeight = 0.30D;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double priorityWeight = 0.10D;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double successRateWeight = 0.05D;
}
