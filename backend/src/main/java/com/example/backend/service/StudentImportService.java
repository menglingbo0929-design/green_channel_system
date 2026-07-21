package com.example.backend.service;

import com.example.backend.model.dto.ImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

public interface StudentImportService {

    /** 导入学生 Excel，返回成功/跳过/错误统计 */
    ImportResult importExcel(MultipartFile file);

    /** 生成导入模板写入输出流 */
    void writeTemplate(OutputStream out);
}
