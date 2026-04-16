package com.atustcchen.javaailongchain4j;

import com.atustcchen.javaailongchain4j.Bean.ChatMessages;
import dev.langchain4j.data.message.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootTest
public class MongoTest {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void test()
    {
        ChatMessages chatMessage = mongoTemplate.findById("690049151ca45eca1652271b", ChatMessages.class);
        System.out.println(chatMessage.getContent());
    }
}
