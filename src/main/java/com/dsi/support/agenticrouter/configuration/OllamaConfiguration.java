package com.dsi.support.agenticrouter.configuration;

import org.apache.hc.core5.util.Timeout;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OllamaHttpProperties.class)
public class OllamaConfiguration {

    @Bean
    public ClientHttpRequestFactory ollamaClientHttpRequestFactory(
        OllamaHttpProperties ollamaHttpProperties
    ) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                                                                                    .withTimeouts(
                                                                                        ollamaHttpProperties.getConnectTimeout(),
                                                                                        ollamaHttpProperties.getReadTimeout()
                                                                                    );

        HttpComponentsClientHttpRequestFactoryBuilder builder = ClientHttpRequestFactoryBuilder.httpComponents()
                                                                                               .withConnectionManagerCustomizer(
                                                                                                   connectionManagerBuilder -> connectionManagerBuilder
                                                                                                       .setMaxConnTotal(ollamaHttpProperties.getPool().getMaxTotal())
                                                                                                       .setMaxConnPerRoute(ollamaHttpProperties.getPool().getMaxPerRoute())
                                                                                               )
                                                                                               .withDefaultRequestConfigCustomizer(
                                                                                                   requestConfigBuilder -> requestConfigBuilder
                                                                                                       .setResponseTimeout(Timeout.of(ollamaHttpProperties.getReadTimeout()))
                                                                                               );

        return builder.build(settings);
    }

    @Bean
    public OllamaApi ollamaApi(
        @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
        RestClient.Builder restClientBuilder,
        WebClient.Builder webClientBuilder,
        ResponseErrorHandler responseErrorHandler,
        ClientHttpRequestFactory ollamaClientHttpRequestFactory
    ) {
        RestClient.Builder ollamaRestClientBuilder = restClientBuilder.clone()
                                                                      .baseUrl(ollamaBaseUrl)
                                                                      .requestFactory(ollamaClientHttpRequestFactory)
                                                                      .defaultStatusHandler(responseErrorHandler);

        WebClient.Builder ollamaWebClientBuilder = webClientBuilder.clone()
                                                                   .baseUrl(ollamaBaseUrl);

        return OllamaApi.builder()
                        .baseUrl(ollamaBaseUrl)
                        .restClientBuilder(ollamaRestClientBuilder)
                        .webClientBuilder(ollamaWebClientBuilder)
                        .responseErrorHandler(responseErrorHandler)
                        .build();
    }
}
