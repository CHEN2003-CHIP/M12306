package com.atustcchen.javaailongchain4j;

import com.atustcchen.javaailongchain4j.assistant.Assistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AIServiceTest {
    @Autowired
    private QwenChatModel qwenChatModel;
    @Test
    public void test(){
        Assistant assistant = AiServices.create(Assistant.class,qwenChatModel);
        String ans=assistant.chat("你好");
        System.out.println(ans);
    }

    @Autowired
    private Assistant assistant;
    @Test
    public void test1(){
        String ans=assistant.chat("你好,青岛");
        System.out.println(ans);
    }
}
