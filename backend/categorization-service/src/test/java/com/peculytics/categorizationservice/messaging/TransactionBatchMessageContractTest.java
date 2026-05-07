package com.peculytics.categorizationservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionBatchMessageContractTest {
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void shouldDeserializeCanonicalTransactionBatchContract() throws Exception {
        TransactionBatchMessage message = OBJECT_MAPPER.readValue(
                new ClassPathResource("contracts/transaction-batch-message.json").getInputStream(),
                TransactionBatchMessage.class
        );

        assertThat(message.analysisId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(message.statementFileId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(message.batchNumber()).isEqualTo(2);
        assertThat(message.totalBatches()).isEqualTo(5);
        assertThat(message.transactions()).hasSize(1);
        assertThat(message.transactions().getFirst().description()).isEqualTo("IFOOD RESTAURANT");
        assertThat(message.transactions().getFirst().amount()).isEqualByComparingTo(new BigDecimal("-45.90"));
        assertThat(message.transactions().getFirst().transactionDate()).isEqualTo(LocalDate.of(2026, 3, 15));
    }
}
