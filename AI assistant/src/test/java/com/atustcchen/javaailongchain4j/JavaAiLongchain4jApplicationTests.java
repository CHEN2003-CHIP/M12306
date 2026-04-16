package com.atustcchen.javaailongchain4j;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.awt.*;
import java.net.URI;

@SpringBootTest
class JavaAiLongchain4jApplicationTests {

    @Autowired
    private QwenChatModel qwenChatModel;
    @Test
    public void contextLoads() {

        String result=qwenChatModel.chat("你好，青岛在哪？");
        System.out.println(result);
    }

    //图片生产
    @Test
    public void testQwenChatModel() {
        WanxImageModel wanxImageModel = WanxImageModel
                .builder()
                .modelName("wanx2.1-t2i-turbo")
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .build();
        Response<Image> response = wanxImageModel.generate("OC渲染，潮玩风格，渐变背景，超现实主义。桃花妖，特写，生宣纸，面部胭脂晕染，背景中式山水画卷，极简主义，留白");
        URI url=response.content().url();
        System.out.println(url);
    }

    @Autowired
    private OpenAiChatModel openAiChatModel;
    @Test
    public void DPcontextLoads() {

        String result=openAiChatModel.chat("你好，青岛在哪？");
        System.out.println(result);
    }
}
