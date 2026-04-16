package com.atustcchen.javaailongchain4j.Bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessages {
    @Id
    private ObjectId id;

    private String memoryId;//当前聊天记录页的唯一标识符

    private String content;//存储当前聊天记录页表的json字符串

}
