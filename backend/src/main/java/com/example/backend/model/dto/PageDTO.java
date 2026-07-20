package com.example.backend.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 所有成员四分页列表共用的分页参数。
 *
 * <p>与 demo 的 PageDTO 保持相同的 {@code pageNo/pageSize} 命名：
 * 前端请求时不传参数会默认查询第 1 页、每页 10 条。</p>
 */
@Data
@Accessors(chain = true)
public class PageDTO {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
