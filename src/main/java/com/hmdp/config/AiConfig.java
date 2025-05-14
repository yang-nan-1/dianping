package com.hmdp.config;





import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${langchain4j.community.dashscope.api-key}")
    private String apiKey;

    @Value("${langchain4j.community.dashscope.model-name}")
    private String modelName;

    @Value("${spring.data.elasticsearch.rest.uris}")
    private String elasticsearchUrl;

    @Bean
    public QwenChatModel qwenChatModel() {
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    @Bean
    public QwenEmbeddingModel getModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .build();
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
