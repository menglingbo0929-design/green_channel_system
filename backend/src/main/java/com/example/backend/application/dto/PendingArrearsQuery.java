package com.example.backend.application.dto;

public record PendingArrearsQuery(int pageNo, int pageSize) {
    public int offset() { return (pageNo - 1) * pageSize; }
    public void validate() { if (pageNo < 1 || pageSize < 1 || pageSize > 100) throw new IllegalArgumentException("pageNo 必须大于 0，pageSize 必须在 1 到 100 之间"); }
}
