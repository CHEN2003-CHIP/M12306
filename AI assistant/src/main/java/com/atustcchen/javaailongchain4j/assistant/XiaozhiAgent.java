package com.atustcchen.javaailongchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(streamingChatModel  = "qwenStreamingChatModel",
           wiringMode = EXPLICIT,
           chatMemoryProvider = "chatMemoryProvider_xiaozhi",
           tools = "apartmentTools",
           contentRetriever = "contentRetrieverXIAO")
public interface XiaozhiAgent {
    @SystemMessage(fromResource = "xiaozhi-prompt-template.txt")
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String userMessage);

}
