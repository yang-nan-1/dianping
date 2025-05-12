package com.hmdp;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;


@SpringBootTest
public class AIChatTest {

    @Resource
    private QwenChatModel qwenChatModel;
    @Resource
    private QwenEmbeddingModel qwenEmbeddingModel;

    @Test
    //测试通话
    public void test() {
        String chat = qwenChatModel.chat("你是谁？");
        System.out.println(chat);
    }

    @Test
    //测试向量转化模型
    public void test1() {
        Response<Embedding> embed = qwenEmbeddingModel.embed("我是qwen");
        System.out.println(embed.content().vector().length);
    }
}