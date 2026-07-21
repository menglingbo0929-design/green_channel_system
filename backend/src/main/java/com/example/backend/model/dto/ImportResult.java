package com.example.backend.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ImportResult {
    private int total;
    private int success;
    private int skipped;
    private List<String> errors;

    public static ImportResult empty() {
        return ImportResult.builder()
                .total(0).success(0).skipped(0)
                .errors(new ArrayList<>())
                .build();
    }
}
