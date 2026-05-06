package com.peculytics.apiservice.controller;

import com.peculytics.apiservice.dto.GetAnalysisResponse;
import com.peculytics.apiservice.dto.ListAnalysesResponse;
import com.peculytics.apiservice.dto.GetAnalysisSummaryResponse;
import com.peculytics.apiservice.dto.GetAnalysisTransactionsResponse;
import com.peculytics.apiservice.usecase.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/analyses")
@RequiredArgsConstructor
public class AnalysisController {
    private final ListAnalysesUseCase listAnalysesUseCase;
    private final GetAnalysisUseCase getAnalysisUseCase;
    private final GetAnalysisTransactionsUseCase getAnalysisTransactionsUseCase;
    private final GetAnalysisSummaryUseCase getAnalysisSummaryUseCase;
    private final DeleteAnalysisUseCase deleteAnalysisUseCase;

    @GetMapping
    public ResponseEntity<List<ListAnalysesResponse>> listAnalyses() {
        List<ListAnalysesResponse> response = listAnalysesUseCase.execute();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetAnalysisResponse> getAnalysis(
            @PathVariable("id") UUID id
    ) {
        GetAnalysisResponse response = getAnalysisUseCase.execute(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<GetAnalysisTransactionsResponse> getTransactions(
            @PathVariable("id") UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size
    ) {
        GetAnalysisTransactionsResponse response = getAnalysisTransactionsUseCase.execute(id, page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<GetAnalysisSummaryResponse> getSummary(
            @PathVariable("id") UUID id
    ) {
        GetAnalysisSummaryResponse response = getAnalysisSummaryUseCase.execute(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteAnalysis(
            @PathVariable("id") UUID id
    ) {
        deleteAnalysisUseCase.execute(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
