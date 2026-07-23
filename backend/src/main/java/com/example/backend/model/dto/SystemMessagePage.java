package com.example.backend.model.dto;

import java.util.List;

public record SystemMessagePage(
        List<SystemMessageItem> records,
        long total,
        int page,
        int size
) {
    public SystemMessagePage {
        records = List.copyOf(records);
    }
}
