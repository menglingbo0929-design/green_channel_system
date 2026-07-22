package com.example.backend.application.dto;

public record PendingArrearsQuery(int pageNo, int pageSize) {
    public int offset() { return (pageNo - 1) * pageSize; }
}
