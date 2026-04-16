package com.atustcchen.javaailongchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import net.bytebuddy.build.Plugin;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class RAGtest {

    @Test
    public void test() {
//        Document doc= FileSystemDocumentLoader.loadDocument("E:/JavaProject/GiteeClone/RAG/shedding_light_paper.pdf",
//                new ApachePdfBoxDocumentParser());
//        System.out.println(doc.text());

        Document doc = FileSystemDocumentLoader.loadDocument("E:/JavaProject/GiteeClone/RAG/testRAG.txt");

        InMemoryEmbeddingStore<TextSegment> store=new InMemoryEmbeddingStore<>();

        DocumentByParagraphSplitter documentByParagraphSplitter=new DocumentByParagraphSplitter(
                300,
                30,
                new HuggingFaceTokenizer()
        );
        //        store.put(doc.text(), doc.id());
        EmbeddingStoreIngestor
                .builder()
                .embeddingStore(store)
        .documentSplitter(documentByParagraphSplitter)
        .build()
        .ingest(doc);

        System.out.println(store);
    }
}
