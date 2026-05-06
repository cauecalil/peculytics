package com.peculytics.categorizationservice.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peculytics.categorizationservice.model.TransactionCategory;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class LangChain4jGeminiTransactionCategorizer implements TransactionCategorizerAi {
    private final ChatModel chatModel;
    private final CategorizationResponseParser responseParser;
    private final ObjectMapper objectMapper;

    @Override
    public CategorizationResponse categorize(CategorizationPrompt prompt) {
        Set<Integer> allowedIndexes = prompt.transactions().stream()
                .map(CategorizationPrompt.TransactionPromptItem::index)
                .collect(Collectors.toSet());

        String requestPrompt;
        try {
            requestPrompt = buildPrompt(prompt);
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Could not serialize AI categorization prompt; falling back to unresolved categories. transactionCount={} errorType={} errorMessage={}",
                    prompt.transactions().size(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );

            return CategorizationResponse.builder()
                    .categoriesByIndex(Map.of())
                    .build();
        }

        String response = chatModel.chat(requestPrompt);

        return responseParser.parse(response, allowedIndexes);
    }

    private String buildPrompt(CategorizationPrompt prompt) throws JsonProcessingException {
        String allowedCategories = objectMapper.writeValueAsString(TransactionCategory.allowedAiCategories());
        String transactionData = objectMapper.writeValueAsString(Map.of("transactions", prompt.transactions()));

        return """
                <instructions>
                Categorize these financial transactions.
                Use only these allowed categories: %s.
                Return only JSON in this format:
                {"categories":[{"index":0,"category":"FOOD"}]}
                </instructions>

                <data>
                %s
                </data>
                """.formatted(allowedCategories, transactionData);
    }
}
