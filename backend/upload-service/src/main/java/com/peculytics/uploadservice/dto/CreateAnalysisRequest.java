package com.peculytics.uploadservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Builder
public record CreateAnalysisRequest(
        @NotBlank(message = "Title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        @NotEmpty(message = "At least one CSV file is required")
        @Size(max = 10, message = "An analysis can contain at most 10 files")
        List<MultipartFile> files,

        List<String> fileTitles
) {}
