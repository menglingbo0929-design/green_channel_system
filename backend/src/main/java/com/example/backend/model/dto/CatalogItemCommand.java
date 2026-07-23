package com.example.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 成员二自有基础配置（欠费项目、礼包物品）的写入参数。 */
public record CatalogItemCommand(
        @NotBlank(message = "名称不能为空") String name,
        @NotNull(message = "启用状态不能为空") Boolean enabled) {
}
