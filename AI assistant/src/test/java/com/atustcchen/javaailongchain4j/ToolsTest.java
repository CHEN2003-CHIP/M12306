package com.atustcchen.javaailongchain4j;

import com.atustcchen.javaailongchain4j.assistant.SeparateChatAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ToolsTest {
    @Autowired
    private SeparateChatAssistant separateChatAssistant;

    @Test
    public void test()
    {
        String  ans=separateChatAssistant.chat(1,"1+1等于几,47777777789的平方根是多少");
        System.out.println(ans);
    }

}
