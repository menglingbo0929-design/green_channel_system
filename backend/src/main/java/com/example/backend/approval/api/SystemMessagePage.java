package com.example.backend.approval.api;

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
