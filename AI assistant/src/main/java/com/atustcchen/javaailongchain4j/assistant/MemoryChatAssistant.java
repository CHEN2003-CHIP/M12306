package com.atustcchen.javaailongchain4j.assistant;

import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode=EXPLICIT,chatModel = "qwenChatModel",chatMemory = "chatMemory")
public interface MemoryChatAssistant {
    String chat(String message);
}
