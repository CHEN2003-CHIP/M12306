package com.atustcchen.javaailongchain4j.Config;

import com.atustcchen.javaailongchain4j.Store.MongoDBChatMemoryStore;
import com.atustcchen.javaailongchain4j.assistant.SeparateChatAssistant;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeparateChatAssistantConfig {
    @Autowired
    private MongoDBChatMemoryStore mongoDBChatMemoryStore;
    @Bean
    public ChatMemoryProvider chatMemoryProvider_xiaozhi() {
        return memoryId -> MessageWindowChatMemory
                .builder()
                //.chatMemoryStore(new InMemoryChatMemoryStore())
                .chatMemoryStore(mongoDBChatMemoryStore)
                .id(memoryId)
                .maxMessages(10)
                .build();
    }
}
