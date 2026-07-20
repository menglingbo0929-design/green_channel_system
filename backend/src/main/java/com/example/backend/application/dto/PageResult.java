package com.example.backend.application.dto;

import java.util.List;

public record PageResult<T>(long total, int pageNo, int pageSize, List<T> records) { }
