package com.peculytics.categorizationservice.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peculytics.categorizationservice.model.TransactionCategory;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LangChain4jGeminiTransactionCategorizerTest {
    @Mock
    private ChatModel chatModel;

    @Mock
    private CategorizationResponseParser responseParser;

    @Test
    void shouldBuildStructuredPromptAndCallChatModel() {
        CategorizationPrompt prompt = prompt();
        CategorizationResponse parsedResponse = new CategorizationResponse(Map.of(0, TransactionCategory.FOOD));
        LangChain4jGeminiTransactionCategorizer categorizer = new LangChain4jGeminiTransactionCategorizer(
                chatModel,
                responseParser,
                new ObjectMapper().findAndRegisterModules()
        );
        when(chatModel.chat(anyString())).thenReturn("{\"categories\":[]}");
        when(responseParser.parse("{\"categories\":[]}", Set.of(0, 2))).thenReturn(parsedResponse);

        CategorizationResponse response = categorizer.categorize(prompt);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatModel).chat(promptCaptor.capture());
        String requestPrompt = promptCaptor.getValue();
        assertThat(requestPrompt)
                .contains("<instructions>")
                .contains("<data>")
                .contains("Use only these allowed categories")
                .contains("\"FOOD\"")
                .contains("\"TRANSPORT\"")
                .contains("\"transactions\"")
                .contains("\"index\":0")
                .contains("\"index\":2")
                .doesNotContain("UNCATEGORIZED");
        assertThat(response).isSameAs(parsedResponse);
    }

    @Test
    void shouldUseAllowedIndexesWhenParsingResponse() {
        CategorizationPrompt prompt = prompt();
        LangChain4jGeminiTransactionCategorizer categorizer = new LangChain4jGeminiTransactionCategorizer(
                chatModel,
                responseParser,
                new ObjectMapper().findAndRegisterModules()
        );
        when(chatModel.chat(anyString())).thenReturn("raw response");
        when(responseParser.parse("raw response", Set.of(0, 2))).thenReturn(new CategorizationResponse(Map.of()));

        categorizer.categorize(prompt);

        verify(responseParser).parse("raw response", Set.of(0, 2));
    }

    @Test
    void shouldReturnEmptyResponseAndNotCallChatModelWhenPromptSerializationFails() throws Exception {
        ObjectMapper objectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        LangChain4jGeminiTransactionCategorizer categorizer = new LangChain4jGeminiTransactionCategorizer(
                chatModel,
                responseParser,
                objectMapper
        );
        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new JsonProcessingException("boom") {});

        CategorizationResponse response = categorizer.categorize(prompt());

        assertThat(response.categoriesByIndex()).isEmpty();
        verify(chatModel, never()).chat(anyString());
    }

    private static CategorizationPrompt prompt() {
        return new CategorizationPrompt(java.util.List.of(
                CategorizationPrompt.TransactionPromptItem.builder()
                        .index(0)
                        .description("Lunch")
                        .amount(new BigDecimal("18.50"))
                        .transactionDate(LocalDate.of(2026, 5, 1))
                        .build(),
                CategorizationPrompt.TransactionPromptItem.builder()
                        .index(2)
                        .description("Taxi")
                        .amount(new BigDecimal("27.00"))
                        .transactionDate(LocalDate.of(2026, 5, 2))
                        .build()
        ));
    }
}
