package com.atustcchen.javaailongchain4j.Config;

import com.atustcchen.javaailongchain4j.Store.MongoDBChatMemoryStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class XiaozhiAgentConfig {
    @Autowired
    private MongoDBChatMemoryStore mongoDBChatMemoryStore;
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory
                .builder()
                //.chatMemoryStore(new InMemoryChatMemoryStore())
                .chatMemoryStore(mongoDBChatMemoryStore)
                .id(memoryId)
                .maxMessages(30)
                .build();
    }

//    @Bean
//    ContentRetriever contentRetrieverXIAO() {
//        Document doc1= FileSystemDocumentLoader.loadDocument("E:/JavaProject/GiteeClone/RAG/testRAG.txt");
//        Document doc2= FileSystemDocumentLoader.loadDocument("E:/JavaProject/GiteeClone/RAG/科室.txt");
//        Document doc3= FileSystemDocumentLoader.loadDocument("E:/JavaProject/GiteeClone/RAG/内科.txt");
//        Document doc4= FileSystemDocumentLoader.loadDocument("E:/JavaProject/GiteeClone/RAG/医生.txt");
//        List<Document> docs= Arrays.asList(doc1,doc2,doc3,doc4);
//
//        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
//
//        EmbeddingStoreIngestor.ingest( docs, embeddingStore);
//
//        return EmbeddingStoreContentRetriever.from(embeddingStore);
//    }
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private EmbeddingStore embeddingStore;

    @Bean
    public ContentRetriever contentRetrieverXIAO() {
        return EmbeddingStoreContentRetriever
                .builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(1)
                .minScore(0.5)
                .build();
    }
}
