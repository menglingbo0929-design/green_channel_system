package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.statistics.StatisticsReportQueryDTO;
import com.example.backend.model.vo.statistics.StatisticsExportFileVO;
import com.example.backend.model.vo.statistics.StatisticsReportPageVO;
import com.example.backend.model.vo.statistics.StatisticsReportRowVO;
import com.example.backend.service.port.StatisticsAccessPort;
import com.example.backend.service.port.StatisticsReportQueryPort;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 6.1.7 核心行为测试：字段白名单和真正的 xlsx 文件生成。 */
class StatisticsReportServiceImplTest {

    private final StatisticsReportQueryPort reportPort = mock(StatisticsReportQueryPort.class);
    private final StatisticsAccessPort accessPort = mock(StatisticsAccessPort.class);
    private final StatisticsReportServiceImpl service = new StatisticsReportServiceImpl(reportPort, accessPort);

    @BeforeEach
    void setUp() {
    }

    @Test
    void keepsRequestedColumnOrder() {
        StatisticsReportRowVO row = sampleRow();
        Page<StatisticsReportRowVO> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(row));
        when(reportPort.queryReportPage(any(), eq(1L))).thenReturn(page);

        StatisticsReportQueryDTO query = new StatisticsReportQueryDTO();
        query.setColumns(List.of("studentNo", "applicationNo"));
        StatisticsReportPageVO result = service.queryDetails(query, 1L);

        assertEquals(List.of("studentNo", "applicationNo"),
                result.getColumns().stream().map(column -> column.getKey()).toList());
        assertEquals(List.of("studentNo", "applicationNo"),
                result.getRecords().getFirst().keySet().stream().toList());

    }

    @Test
    void exportsRealXlsxWithSelectedHeadersAndData() throws Exception {
        Page<StatisticsReportRowVO> page = new Page<>(1, 500, 1);
        page.setRecords(List.of(sampleRow()));
        when(reportPort.queryReportPage(any(), eq(1L))).thenReturn(page);

        StatisticsReportQueryDTO query = new StatisticsReportQueryDTO();
        query.setColumns(List.of("applicationNo", "confirmedAmount"));
        StatisticsExportFileVO file = service.exportExcel(query, 1L);

        assertTrue(file.getFileName().endsWith(".xlsx"));
        assertTrue(file.getContent().length > 0);

        // 用 POI 重新打开导出结果，证明它不是改扩展名的 CSV 或损坏文件。
        XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.getContent()));
        assertEquals("申请编号", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        assertEquals("确认金额", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
        assertEquals("GC202607200001", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
        assertEquals(1200.00, workbook.getSheetAt(0).getRow(1).getCell(1).getNumericCellValue());
        workbook.close();
        verify(accessPort).checkSchoolStatisticsUser(1L);
    }

    private StatisticsReportRowVO sampleRow() {
        StatisticsReportRowVO row = new StatisticsReportRowVO();
        row.setApplicationId(1001L);
        row.setApplicationNo("GC202607200001");
        row.setStudentNo("20260001");
        row.setStudentName("张三");
        row.setCollegeName("计算机学院");
        row.setApplicationStatus("COMPLETED");
        row.setConfirmedAmount(new BigDecimal("1200.00"));
        row.setApplicationTime(LocalDateTime.of(2026, 7, 20, 9, 30));
        return row;
    }
}
