package com.dsi.support.agenticrouter.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class LlmPromptCaller {

    private final ChatClientFactory chatClientFactory;
    private final PromptService promptService;

    public ChatClient.CallResponseSpec call(
        ChatModel chatModel,
        Consumer<ChatClient.PromptUserSpec> userSpecConsumer
    ) {
        return chatClientFactory.create(chatModel)
                                .prompt()
                                .system(promptService.getSystemPrompt())
                                .user(userSpecConsumer)
                                .call();
    }

    public Flux<String> streamContent(
        ChatModel chatModel,
        Consumer<ChatClient.PromptUserSpec> userSpecConsumer
    ) {
        return chatClientFactory.create(chatModel)
                                .prompt()
                                .system(promptService.getSystemPrompt())
                                .user(userSpecConsumer)
                                .stream()
                                .content();
    }
}
