package com.atustcchen.javaailongchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class EmbeddingTest {
    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore embeddingStore;

    @Test
    public void test(){
        Response<Embedding> embed=embeddingModel.embed("你好，世界！");
        System.out.println("向量维度："+embed.content().vector().length);
        System.out.println("向量输出："+embed.toString());
    }

    @Test
    public void test2(){
        TextSegment segment=TextSegment.from("我爱，ustc！");
        Embedding embedding=embeddingModel.embed(segment).content();

        embeddingStore.add(embedding,segment);
    }

    @Test
    public void test3(){
        //转成向量
        Embedding embedding=embeddingModel.embed("你最爱什么").content();
        //创建搜索请求
        EmbeddingSearchRequest searchRequest=EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(1)//最相似
                .minScore(0.8)
                .build();
        //在向量库中搜索
        EmbeddingSearchResult<TextSegment> searchResult=embeddingStore.search(searchRequest);

        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches ();
        if(matches.isEmpty()){
            System.out.println("没有匹配的文本");
            return;
        }
        else{
            System.out.println("匹配到的文本：");
            //从匹配列表中取出第一个
            EmbeddingMatch<TextSegment> match=searchResult.matches().get(0);

            System.out.println(match.embedded().text());
            System.out.println(match.score());
        }

    }

    @Test
    public void test4(){
        //Document doc1= FileSystemDocumentLoader.loadDocument("E:/JavaProject/GiteeClone/RAG/医生.txt");
        //Document doc2= FileSystemDocumentLoader.loadDocument("E:/JavaProject/GiteeClone/RAG/科室.txt");
        Document doc3= FileSystemDocumentLoader.loadDocument("E:/JavaProject/stapartment-aiassistant/mydatabase.txt");
        List<Document> docs= Arrays.asList(doc3);

        EmbeddingStoreIngestor
                .builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(docs);
    }
}
