package com.example.backend.model.dto;

/** 欠费项目或礼包物品的读取模型。 */
public record CatalogItemView(Long id, String name, boolean enabled) {
}
