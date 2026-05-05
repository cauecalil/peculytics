package com.peculytics.uploadservice.controller;

import com.peculytics.uploadservice.dto.CreateAnalysisRequest;
import com.peculytics.uploadservice.dto.CreateAnalysisResponse;
import com.peculytics.uploadservice.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analyses")
@RequiredArgsConstructor
public class AnalysisController {
    private final AnalysisService analysisService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateAnalysisResponse> upload(
            @Valid @ModelAttribute CreateAnalysisRequest request
    ) {
        CreateAnalysisResponse response = analysisService.createAnalysis(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
