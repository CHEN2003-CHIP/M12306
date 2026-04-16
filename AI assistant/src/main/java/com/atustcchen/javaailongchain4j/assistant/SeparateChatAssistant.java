package com.atustcchen.javaailongchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT,chatModel = "qwenChatModel",chatMemoryProvider = "chatMemoryProvider",
           tools="calculatorTools")
public interface SeparateChatAssistant {

    @SystemMessage(fromResource = "my-prompt-template.txt")
    String chat(@MemoryId int memoryId,@UserMessage String message);
}
