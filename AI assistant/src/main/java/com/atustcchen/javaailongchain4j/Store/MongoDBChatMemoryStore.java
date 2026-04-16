package com.atustcchen.javaailongchain4j.Store;

import com.atustcchen.javaailongchain4j.Bean.ChatMessages;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class MongoDBChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<ChatMessage> getMessages(Object o) {
        Criteria criteria = Criteria.where("memoryId").is(o);
        Query query = new Query(criteria);
        ChatMessages messages = mongoTemplate.findOne(query, ChatMessages.class);

        if(messages == null){
            return new LinkedList<>();
        }

        return ChatMessageDeserializer.messagesFromJson(messages.getContent());

    }

    @Override
    public void updateMessages(Object o, List<ChatMessage> list) {
        Criteria criteria = Criteria.where("memoryId").is(o);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("content", ChatMessageSerializer.messagesToJson(list));
        mongoTemplate.upsert(query, update, ChatMessages.class);
    }

    @Override
    public void deleteMessages(Object o) {
        Criteria criteria = Criteria.where("memoryId").is(o);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);
    }
}
