package com.example.backend.model.dto;

public record PendingArrearsQuery(int pageNo, int pageSize) {
    public int offset() { return (pageNo - 1) * pageSize; }
}
