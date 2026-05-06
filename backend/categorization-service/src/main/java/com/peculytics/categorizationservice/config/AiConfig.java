package com.peculytics.categorizationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peculytics.categorizationservice.ai.CategorizationResponseParser;
import com.peculytics.categorizationservice.ai.FallbackTransactionCategorizerAi;
import com.peculytics.categorizationservice.ai.LangChain4jGeminiTransactionCategorizer;
import com.peculytics.categorizationservice.ai.TransactionCategorizerAi;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class AiConfig {
    @Bean
    CategorizationResponseParser categorizationResponseParser(ObjectMapper objectMapper) {
        return new CategorizationResponseParser(objectMapper);
    }

    @Bean
    TransactionCategorizerAi transactionCategorizerAi(
            GeminiProperties geminiProperties,
            CategorizationResponseParser responseParser,
            ObjectMapper objectMapper
    ) {
        if (!StringUtils.hasText(geminiProperties.apiKey())) {
            return new FallbackTransactionCategorizerAi();
        }

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(geminiProperties.apiKey())
                .modelName(geminiProperties.model())
                .returnThinking(false)
                .build();

        return new LangChain4jGeminiTransactionCategorizer(chatModel, responseParser, objectMapper);
    }
}
