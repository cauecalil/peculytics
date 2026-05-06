package com.peculytics.categorizationservice.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peculytics.categorizationservice.model.TransactionCategory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CategorizationResponseParserTest {
    private final CategorizationResponseParser parser = new CategorizationResponseParser(new ObjectMapper());

    @Test
    void shouldParseCategoriesObjectResponse() {
        CategorizationResponse response = parser.parse(
                "{\"categories\":[{\"index\":0,\"category\":\"FOOD\"}]}",
                Set.of(0)
        );

        assertThat(response.categoriesByIndex())
                .containsEntry(0, TransactionCategory.FOOD);
    }

    @Test
    void shouldParseDirectArrayResponse() {
        CategorizationResponse response = parser.parse(
                "[{\"index\":1,\"category\":\"TRANSPORT\"}]",
                Set.of(1)
        );

        assertThat(response.categoriesByIndex())
                .containsEntry(1, TransactionCategory.TRANSPORT);
    }

    @Test
    void shouldIgnoreIndexThatIsNotAllowed() {
        CategorizationResponse response = parser.parse(
                "{\"categories\":[{\"index\":2,\"category\":\"HEALTH\"}]}",
                Set.of(0, 1)
        );

        assertThat(response.categoriesByIndex()).isEmpty();
    }

    @Test
    void shouldIgnoreNullBlankInvalidAndUncategorizedCategories() {
        CategorizationResponse response = parser.parse(
                """
                {"categories":[
                  {"index":0,"category":null},
                  {"index":1,"category":"   "},
                  {"index":2,"category":"NOT_A_CATEGORY"},
                  {"index":3,"category":"UNCATEGORIZED"}
                ]}
                """,
                Set.of(0, 1, 2, 3)
        );

        assertThat(response.categoriesByIndex()).isEmpty();
    }

    @Test
    void shouldKeepValidCategoriesWhenOtherItemsAreInvalid() {
        CategorizationResponse response = parser.parse(
                """
                {"categories":[
                  {"index":0,"category":"FOOD"},
                  {"index":1,"category":"INVALID"},
                  {"index":2,"category":"SHOPPING"}
                ]}
                """,
                Set.of(0, 1, 2)
        );

        assertThat(response.categoriesByIndex())
                .containsEntry(0, TransactionCategory.FOOD)
                .containsEntry(2, TransactionCategory.SHOPPING)
                .doesNotContainKey(1);
    }

    @Test
    void shouldReturnEmptyMapForNullBlankAndInvalidJsonResponses() {
        assertThat(parser.parse(null, Set.of(0)).categoriesByIndex()).isEmpty();
        assertThat(parser.parse("   ", Set.of(0)).categoriesByIndex()).isEmpty();
        assertThat(parser.parse("not json", Set.of(0)).categoriesByIndex()).isEmpty();
    }

    @Test
    void shouldNormalizeCategoryWithTrimAndUppercase() {
        CategorizationResponse response = parser.parse(
                "{\"categories\":[{\"index\":0,\"category\":\"  groceries  \"}]}",
                Set.of(0)
        );

        assertThat(response.categoriesByIndex())
                .containsEntry(0, TransactionCategory.GROCERIES);
    }
}
