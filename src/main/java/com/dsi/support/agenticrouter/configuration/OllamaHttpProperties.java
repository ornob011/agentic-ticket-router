package com.dsi.support.agenticrouter.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "ollama.http")
public class OllamaHttpProperties {

    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(600);
    private Pool pool = new Pool();

    @Getter
    @Setter
    public static class Pool {
        private int maxTotal = 200;
        private int maxPerRoute = 50;
    }
}
