package com.example.backend.model.vo.schoolproxy;

import lombok.Data;

/** 创建草稿或正式提交后返回的申请摘要。 */
@Data
public class SchoolProxyApplicationVO {
    private Long applicationId;
    private String applicationNo;
    private String source;
    private String status;
    private Integer version;
}
