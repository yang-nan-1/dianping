package com.hmdp.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${langchain4j.community.dashscope.api-key}")
    private String apiKey;

    @Value("${langchain4j.community.dashscope.model-name}")
    private String modelName;

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
}
