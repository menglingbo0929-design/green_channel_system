package com.example.backend.model.vo.voucher;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 前端预览、学生查看和浏览器打印共用的欠费确认单格式。 */
@Data
public class ArrearsVoucherVO {
    private String voucherNo;
    private Long applicationId;
    private String studentNo;
    private String studentName;
    private String collegeName;
    private String majorName;
    private String gradeName;
    private String className;
    private List<ArrearsVoucherItemVO> arrearsItems;
    private BigDecimal appliedAmount;
    private BigDecimal confirmedAmount;
    private LocalDateTime confirmedTime;
    private Long confirmUserId;
    private String confirmUserName;
    private String printTitle;
    private LocalDateTime printTime;
}
