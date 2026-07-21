package com.example.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginByCodeRequest {
    @NotBlank
    private String phone;
    @NotBlank
    private String code;
}
