package com.example.backend.model.vo.voucher;

import lombok.Data;
import java.util.List;

/** 成员二批量返回的申请与学生快照；成员四只读，不直接访问申请或学生表。 */
@Data
public class ArrearsVoucherApplicantSnapshot {
    private Long applicationId;
    private String studentNo;
    private String studentName;
    private String collegeName;
    private String majorName;
    private String gradeName;
    private String className;
    private List<ArrearsVoucherItemVO> arrearsItems;
}
