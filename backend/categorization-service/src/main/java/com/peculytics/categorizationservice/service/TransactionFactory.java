package com.peculytics.categorizationservice.service;

import com.peculytics.categorizationservice.categorization.CategorizationResult;
import com.peculytics.categorizationservice.messaging.TransactionBatchMessage;
import com.peculytics.categorizationservice.model.Analysis;
import com.peculytics.categorizationservice.model.StatementFile;
import com.peculytics.categorizationservice.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TransactionFactory {
    public List<Transaction> createAll(
            TransactionBatchMessage message,
            Analysis analysis,
            StatementFile statementFile,
            Map<Integer, CategorizationResult> results
    ) {
        List<Transaction> transactions = new ArrayList<>();

        for (int index = 0; index < message.transactions().size(); index++) {
            transactions.add(create(message.transactions().get(index), analysis, statementFile, results.get(index)));
        }

        return transactions;
    }

    private static Transaction create(
            TransactionBatchMessage.TransactionMessage source,
            Analysis analysis,
            StatementFile statementFile,
            CategorizationResult result
    ) {
        return Transaction.categorized(
                analysis,
                statementFile,
                source.description(),
                source.amount(),
                source.transactionDate(),
                result.category(),
                result.source()
        );
    }
}
