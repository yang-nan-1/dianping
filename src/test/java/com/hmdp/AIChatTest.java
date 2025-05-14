package com.hmdp;

import com.hmdp.config.AiConfig;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;

import jakarta.annotation.Resource;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;


@SpringBootTest
public class AIChatTest {

    @Resource
    private AiConfig.QwenAI qwenModel;
    @Resource
    private QwenEmbeddingModel qwenEmbeddingModel;

    @Resource
    private ElasticsearchEmbeddingStore elasticsearchEmbeddingStore;


    @Test
    //测试通话
    public void test() {
        String chat = qwenModel.chat("你是谁？");
        System.out.println(chat);
    }

    @Test
    //测试向量转化模型
    public void test1() {
        Response<Embedding> embed = qwenEmbeddingModel.embed("我是qwen");
        System.out.println(embed.content().toString());
        System.out.println(embed.toString());
        //这里是最重要的，得到了转换后的向量维度，由于硬件设置的限制当前的编码模型已经是极限了
        System.out.println(embed.content().vector().length);
    }


    @Test
    //存储向量数据到向量数据库中，并查询相似内容
    public void test2() {
        // Step 1: 添加一些文本段
        TextSegment segment1 = TextSegment.from("""
                预订航班：
                - 通过我们的网站或移动应用程序预订。
                - 预订时需要全额付款。
                - 确保个人信息（姓名、ID 等）的准确性，因为更正可能会产生 25 的费用。
                """);
        Embedding embedding1 = qwenEmbeddingModel.embed(segment1).content();
        elasticsearchEmbeddingStore.add(embedding1, segment1);

        TextSegment segment2 = TextSegment.from("""
                取消预订：
                - 最晚在航班起飞前 48 小时取消。
                - 取消费用：经济舱 75 美元，豪华经济舱 50 美元，商务舱 25 美元。
                - 退款将在 7 个工作日内处理。
                """);
        Embedding embedding2 = qwenEmbeddingModel.embed(segment2).content();
        elasticsearchEmbeddingStore.add(embedding2, segment2);


        // Step 2: 查询相似内容
        Embedding queryEmbedding = qwenEmbeddingModel.embed("退票要多少钱").content();
        EmbeddingSearchResult<TextSegment> result = elasticsearchEmbeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .build()
        );

        // 处理结果
        System.out.println("相似度: " + result.matches().get(0).score());
        System.out.println("内容: " + result.matches().get(0).embedded().text());
    }

    @Test
    //测试会话记忆，这里我设置了15轮，还有流式输出
    public void memeryChat() throws Exception{
        String chat = qwenModel.chat("我的名字叫曹操");
        System.out.println(chat);
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        TokenStream stream = qwenModel.streamResoponse("我的名字是什么？,你必须回答我的名字是什么？");

        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        stream.onPartialResponse(System.out::print)
                .onCompleteResponse(futureResponse::complete)
                .onError(futureResponse::completeExceptionally)
                .start();

        ChatResponse chatResponse = futureResponse.get(30, SECONDS);
        System.out.println("\n" + chatResponse);
    }
}