package com.atustcchen.javaailongchain4j;

import com.atustcchen.javaailongchain4j.assistant.Assistant;
import com.atustcchen.javaailongchain4j.assistant.MemoryChatAssistant;
import com.atustcchen.javaailongchain4j.assistant.SeparateChatAssistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
public class MemoryTest {

    @Autowired
    private QwenChatModel qwenChatModel;
    @Test
    public void test()
    {

        UserMessage message1 = UserMessage.userMessage("我是CH,要去青岛玩");
        ChatResponse chatResponse=qwenChatModel.chat(message1);
        AiMessage aiMessage1=chatResponse.aiMessage();
        System.out.println(aiMessage1.text());

        UserMessage message2 = UserMessage.userMessage("who am i and where am i going");
        ChatResponse chatResponse2=qwenChatModel.chat(Arrays.asList(message1,aiMessage1, message2));
        AiMessage aiMessage2=chatResponse2.aiMessage();
        System.out.println(aiMessage2.text());


    }

    @Test
    public void test1(){
        MessageWindowChatMemory chatMemory=MessageWindowChatMemory.withMaxMessages(10);

        Assistant assistant= AiServices
                .builder(Assistant.class)
                .chatLanguageModel(qwenChatModel)
                .chatMemory(chatMemory)
                .build();
        String ans1=assistant.chat("我是CH,要去青岛玩");
        System.out.println(ans1);
        String ans2=assistant.chat("who am i and where am i going");
        System.out.println(ans2);

    }

    @Autowired
    private MemoryChatAssistant assistant;
    @Test
    public void test2(){
        String ans1= assistant.chat("i am ch, i want to go to qingdao");
        System.out.println(ans1);
        String ans2= assistant.chat("who am i and where am i going");
        System.out.println(ans2);

    }

    @Autowired
    private SeparateChatAssistant separateAssistant;
    @Test
    public void test3(){
        String ans1= separateAssistant.chat(4,"告诉我今天几号");
        System.out.println(ans1);



    }

}
