package com.example.backend.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateArrearsRequest(@NotNull Integer version, @NotEmpty List<@Valid ArrearsItemCommand> items) { }
