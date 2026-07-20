package com.example.backend.model.vo.statistics;

import lombok.Data;

/** 按爱心礼包物品统计的申请数量；同一申请对同一物品只计一次。 */
@Data
public class GiftItemApplicationCountVO {
    private Long giftItemId;
    private String giftItemName;
    private Long applicationCount;
}
