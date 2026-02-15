package com.dsi.support.agenticrouter.service.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class ChatClientFactory {

    public ChatClient create(
        ChatModel chatModel
    ) {
        return ChatClient.builder(chatModel)
                         .build();
    }
}
