package com.example.backend.model.dto.schoolproxy;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 一项由学校代选的礼包物品；库存和名额由成员二在提交时处理。 */
@Data
public class SchoolProxyGiftItemDTO {
    @NotNull private Long giftItemId;
    @NotNull @Positive private Integer quantity;
}
