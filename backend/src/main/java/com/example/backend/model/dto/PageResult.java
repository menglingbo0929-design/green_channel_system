package com.example.backend.model.dto;

import java.util.List;

public record PageResult<T>(long total, int pageNo, int pageSize, List<T> records) { }
