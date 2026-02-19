package com.dsi.support.agenticrouter.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;

import javax.sql.DataSource;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "ticket.chat-memory")
@ConditionalOnProperty(
    prefix = "ticket.chat-memory",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class TicketChatMemoryConfiguration {

    private boolean enabled = true;

    @Min(5)
    @Max(100)
    private int ticketHistoryMaxMessages = 20;

    @Min(5)
    @Max(100)
    private int customerContextMaxMessages = 30;

    @Min(5)
    @Max(100)
    private int agentPatternMaxMessages = 50;

    @Bean
    public JdbcChatMemoryRepositoryDialect chatMemoryRepositoryDialect(
        DataSource dataSource
    ) {
        return JdbcChatMemoryRepositoryDialect.from(
            dataSource
        );
    }

    @Bean
    public JdbcChatMemoryRepository jdbcChatMemoryRepository(
        JdbcTemplate jdbcTemplate,
        JdbcChatMemoryRepositoryDialect dialect
    ) {
        return JdbcChatMemoryRepository.builder()
                                       .jdbcTemplate(jdbcTemplate)
                                       .dialect(dialect)
                                       .build();
    }

    @Bean
    public ChatMemory ticketHistoryChatMemory(
        JdbcChatMemoryRepository repository
    ) {
        return buildChatMemory(
            repository,
            ticketHistoryMaxMessages
        );
    }

    @Bean
    public ChatMemory customerContextChatMemory(
        JdbcChatMemoryRepository repository
    ) {
        return buildChatMemory(
            repository,
            customerContextMaxMessages
        );
    }

    @Bean
    public ChatMemory agentPatternChatMemory(
        JdbcChatMemoryRepository repository
    ) {
        return buildChatMemory(
            repository,
            agentPatternMaxMessages
        );
    }

    private ChatMemory buildChatMemory(
        JdbcChatMemoryRepository repository,
        int maxMessages
    ) {
        return MessageWindowChatMemory.builder()
                                      .chatMemoryRepository(repository)
                                      .maxMessages(maxMessages)
                                      .build();
    }
}
