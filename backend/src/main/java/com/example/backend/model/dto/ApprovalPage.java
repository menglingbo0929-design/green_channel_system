package com.example.backend.model.dto;

import java.util.List;

/** Stable pagination envelope used by the approval workbench. */
public record ApprovalPage<T>(List<T> records, long total, int page, int size) {

    public ApprovalPage {
        records = records == null ? List.of() : List.copyOf(records);
    }

    public static <T> ApprovalPage<T> empty(int page, int size) {
        return new ApprovalPage<>(List.of(), 0, page, size);
    }
}
