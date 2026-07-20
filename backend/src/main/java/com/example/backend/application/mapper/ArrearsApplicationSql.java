package com.example.backend.application.mapper;

import java.util.List;
import java.util.Map;

public final class ArrearsApplicationSql {
    private ArrearsApplicationSql() { }
    public static String itemsByApplicationIds(Map<String, Object> parameters) {
        @SuppressWarnings("unchecked") List<Long> ids = (List<Long>) parameters.get("applicationIds");
        String placeholders = java.util.stream.IntStream.range(0, ids.size())
                .mapToObj(index -> "#{applicationIds[" + index + "]}").collect(java.util.stream.Collectors.joining(","));
        return "SELECT aa.application_id, aa.fee_item_id, fi.item_name fee_item_name, aa.declared_amount " +
                "FROM arrears_application aa JOIN fee_item fi ON fi.id=aa.fee_item_id AND fi.deleted=0 " +
                "WHERE aa.deleted=0 AND aa.application_id IN (" + placeholders + ") ORDER BY aa.application_id,aa.id";
    }
}
