package com.peculytics.uploadservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionBatchMessageContractTest {
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void shouldSerializeToCanonicalTransactionBatchContract() throws Exception {
        JsonNode expectedPayload = OBJECT_MAPPER.readTree(
                new ClassPathResource("contracts/transaction-batch-message.json").getInputStream()
        );

        JsonNode actualPayload = OBJECT_MAPPER.valueToTree(message());

        assertThat(OBJECT_MAPPER.writeValueAsString(actualPayload))
                .isEqualTo(OBJECT_MAPPER.writeValueAsString(expectedPayload));
    }

    private static TransactionBatchMessage message() {
        return TransactionBatchMessage.builder()
                .analysisId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .statementFileId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .batchNumber(2)
                .totalBatches(5)
                .transactions(List.of(transaction("IFOOD RESTAURANT", "-45.90", LocalDate.of(2026, 3, 15))))
                .build();
    }

    private static TransactionBatchMessage.TransactionMessage transaction(
            String description,
            String amount,
            LocalDate transactionDate
    ) {
        return TransactionBatchMessage.TransactionMessage.builder()
                .description(description)
                .amount(new BigDecimal(amount))
                .transactionDate(transactionDate)
                .build();
    }
}
