package com.example.backend.model.vo.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Service 交给 Controller 的 Excel 文件名和二进制内容。 */
@Data
@AllArgsConstructor
public class StatisticsExportFileVO {
    private String fileName;
    private byte[] content;
}
