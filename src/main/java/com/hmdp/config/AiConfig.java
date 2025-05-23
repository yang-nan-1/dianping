package com.hmdp.config;


import com.hmdp.service.AIToolsService;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import jakarta.annotation.Resource;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Resource
    private AIToolsService aiToolsService;

    @Value("${spring.data.elasticsearch.rest.uris}")
    private String elasticsearchUrl;

    public interface QwenAI {
        //普通对话
        String chat(@UserMessage String question, @MemoryId int memoryId);

        //流式响应
        TokenStream streamResoponse(@UserMessage String question, @MemoryId int memoryId);
    }

    @Bean
    public QwenAI qwenModel(QwenStreamingChatModel qwenStreamingChatModel, QwenChatModel qwenChatModel) {



        QwenAI model = AiServices.builder(QwenAI.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                                .maxMessages(15)
                                .id(memoryId)
                                .build()
                        )
                .tools(aiToolsService)
                .build();
        return model;
    }


    @Bean
    public ElasticsearchEmbeddingStore getStore() {
        // 创建Elasticsearch RestClient
        RestClient restClient = RestClient
                .builder(HttpHost.create(elasticsearchUrl))
                .build();

        // 配置 KNN 查询的参数
        ElasticsearchConfigurationKnn configuration = ElasticsearchConfigurationKnn.builder()
                .numCandidates(5)// 设置 k 值
                .build();

        //默认使用近似KNN查询
        ElasticsearchEmbeddingStore store = ElasticsearchEmbeddingStore.builder()
                .restClient(restClient)
                .configuration(configuration)
                .build();
        return store;
    }

}
