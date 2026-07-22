package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.statistics.StatisticsReportQueryDTO;
import com.example.backend.model.enums.statistics.StatisticsReportColumn;
import com.example.backend.model.vo.statistics.StatisticsExportFileVO;
import com.example.backend.model.vo.statistics.StatisticsReportColumnVO;
import com.example.backend.model.vo.statistics.StatisticsReportPageVO;
import com.example.backend.model.vo.statistics.StatisticsReportPrintVO;
import com.example.backend.model.vo.statistics.StatisticsReportRowVO;
import com.example.backend.service.IStatisticsReportService;
import com.example.backend.service.port.StatisticsAccessPort;
import com.example.backend.service.port.StatisticsReportQueryPort;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 6.1.7 报表明细、历史记录、Excel 和打印演示实现。
 *
 * <p>代码保持视频中的直接调用风格：权限 Port → 明细 Port → 组装结果。
 * 没有自定义异常包装、导出数量限制或额外边界拦截。字段来自页面固定复选框，
 * Excel 使用 SXSSFWorkbook 写出真正的 xlsx 文件。</p>
 */
@Service
public class StatisticsReportServiceImpl implements IStatisticsReportService {

    /** Excel 分页读取大小；它只决定每次读取多少，不限制导出总数。 */
    private static final int EXPORT_PAGE_SIZE = 500;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ObjectProvider<StatisticsReportQueryPort> reportQueryPortProvider;

    @Autowired
    private ObjectProvider<StatisticsAccessPort> statisticsAccessPortProvider;

    @Override
    public StatisticsReportPageVO queryDetails(
            StatisticsReportQueryDTO query,
            Long currentUserId
    ) {
        StatisticsReportQueryDTO actualQuery = query == null
                ? new StatisticsReportQueryDTO()
                : query;
        statisticsAccessPortProvider.getObject()
                .checkSchoolStatisticsUser(currentUserId);

        List<StatisticsReportColumn> columns = resolveColumns(actualQuery.getColumns());
        Page<StatisticsReportRowVO> source = reportQueryPortProvider.getObject()
                .queryReportPage(actualQuery, currentUserId);
        return convertPage(source, columns);
    }

    @Override
    public StatisticsReportPageVO queryHistory(
            StatisticsReportQueryDTO query,
            Long currentUserId
    ) {
        return queryDetails(query, currentUserId);
    }

    @Override
    public StatisticsReportPrintVO buildPrintData(
            StatisticsReportQueryDTO query,
            Long currentUserId
    ) {
        StatisticsReportQueryDTO actualQuery = query == null
                ? new StatisticsReportQueryDTO()
                : query;
        statisticsAccessPortProvider.getObject()
                .checkSchoolStatisticsUser(currentUserId);

        List<StatisticsReportColumn> columns = resolveColumns(actualQuery.getColumns());
        List<StatisticsReportRowVO> rows = loadAllRows(actualQuery, currentUserId);

        StatisticsReportPrintVO result = new StatisticsReportPrintVO();
        result.setTitle("高校绿色通道统计报表");
        result.setGeneratedAt(LocalDateTime.now());
        result.setColumns(toColumnVOs(columns));
        result.setRecords(convertRows(rows, columns));
        result.setTotal(rows.size());
        return result;
    }

    /**
     * 直接生成 Excel 二进制。
     *
     * <p>SXSSFWorkbook 只保留最近 100 行，查询 Port 每次提供 500 行；
     * 这样演示数据和后续较大数据量都使用同一套代码。</p>
     */
    @Override
    public StatisticsExportFileVO exportExcel(
            StatisticsReportQueryDTO query,
            Long currentUserId
    ) throws IOException {
        StatisticsReportQueryDTO actualQuery = query == null
                ? new StatisticsReportQueryDTO()
                : query;
        statisticsAccessPortProvider.getObject()
                .checkSchoolStatisticsUser(currentUserId);

        List<StatisticsReportColumn> columns = resolveColumns(actualQuery.getColumns());
        StatisticsReportQueryPort reportPort = reportQueryPortProvider.getObject();
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        SXSSFSheet sheet = workbook.createSheet("统计明细");
        writeHeader(sheet, columns, createHeaderStyle(workbook));

        StatisticsReportQueryDTO pageQuery = copyQuery(actualQuery);
        pageQuery.setPageNo(1);
        pageQuery.setPageSize(EXPORT_PAGE_SIZE);
        int excelRowIndex = 1;

        Page<StatisticsReportRowVO> page = reportPort
                .queryReportPage(pageQuery, currentUserId);
        while (!page.getRecords().isEmpty()) {
            for (StatisticsReportRowVO row : page.getRecords()) {
                writeExcelRow(sheet, excelRowIndex++, row, columns);
            }
            if (pageQuery.getPageNo() >= page.getPages()) {
                break;
            }
            pageQuery.setPageNo(pageQuery.getPageNo() + 1);
            page = reportPort.queryReportPage(pageQuery, currentUserId);
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columns.size() - 1));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        String fileName = "statistics-report-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ".xlsx";
        return new StatisticsExportFileVO(fileName, outputStream.toByteArray());
    }

    private StatisticsReportPageVO convertPage(
            Page<StatisticsReportRowVO> source,
            List<StatisticsReportColumn> columns
    ) {
        StatisticsReportPageVO result = new StatisticsReportPageVO();
        result.setColumns(toColumnVOs(columns));
        result.setRecords(convertRows(source.getRecords(), columns));
        result.setTotal(source.getTotal());
        result.setPageNo(source.getCurrent());
        result.setPageSize(source.getSize());
        result.setPages(source.getPages());
        return result;
    }

    /** 打印复用分页查询，直到取得当前筛选条件下的全部记录。 */
    private List<StatisticsReportRowVO> loadAllRows(
            StatisticsReportQueryDTO source,
            Long currentUserId
    ) {
        StatisticsReportQueryDTO pageQuery = copyQuery(source);
        pageQuery.setPageNo(1);
        pageQuery.setPageSize(EXPORT_PAGE_SIZE);

        List<StatisticsReportRowVO> rows = new ArrayList<>();
        Page<StatisticsReportRowVO> page = reportQueryPortProvider.getObject()
                .queryReportPage(pageQuery, currentUserId);
        while (!page.getRecords().isEmpty()) {
            rows.addAll(page.getRecords());
            if (pageQuery.getPageNo() >= page.getPages()) {
                break;
            }
            pageQuery.setPageNo(pageQuery.getPageNo() + 1);
            page = reportQueryPortProvider.getObject()
                    .queryReportPage(pageQuery, currentUserId);
        }
        return rows;
    }

    /** 页面字段来自固定复选框；无法识别的值直接忽略。 */
    private List<StatisticsReportColumn> resolveColumns(List<String> requestedColumns) {
        if (requestedColumns == null || requestedColumns.isEmpty()) {
            return StatisticsReportColumn.defaultColumns();
        }

        LinkedHashSet<StatisticsReportColumn> result = new LinkedHashSet<>();
        for (String rawColumn : requestedColumns) {
            for (String key : rawColumn.split(",")) {
                StatisticsReportColumn column = StatisticsReportColumn.fromKey(key.trim());
                if (column != null) {
                    result.add(column);
                }
            }
        }
        return result.isEmpty()
                ? StatisticsReportColumn.defaultColumns()
                : new ArrayList<>(result);
    }

    private List<StatisticsReportColumnVO> toColumnVOs(
            List<StatisticsReportColumn> columns
    ) {
        return columns.stream()
                .map(column -> new StatisticsReportColumnVO(
                        column.getKey(), column.getTitle()))
                .toList();
    }

    private List<LinkedHashMap<String, Object>> convertRows(
            List<StatisticsReportRowVO> rows,
            List<StatisticsReportColumn> columns
    ) {
        List<LinkedHashMap<String, Object>> result = new ArrayList<>();
        for (StatisticsReportRowVO row : rows) {
            LinkedHashMap<String, Object> record = new LinkedHashMap<>();
            for (StatisticsReportColumn column : columns) {
                record.put(column.getKey(), columnValue(row, column));
            }
            result.add(record);
        }
        return result;
    }

    /** 固定 switch 读取列值，不使用反射。 */
    private Object columnValue(
            StatisticsReportRowVO row,
            StatisticsReportColumn column
    ) {
        Object value = switch (column) {
            case APPLICATION_ID -> row.getApplicationId();
            case APPLICATION_NO -> row.getApplicationNo();
            case STUDENT_NO -> row.getStudentNo();
            case STUDENT_NAME -> row.getStudentName();
            case COLLEGE_NAME -> row.getCollegeName();
            case MAJOR_NAME -> row.getMajorName();
            case GRADE_NAME -> row.getGradeName();
            case CLASS_NAME -> row.getClassName();
            case APPLICATION_TYPE -> row.getApplicationType();
            case BATCH_TYPE -> row.getBatchType();
            case BATCH_ID -> row.getBatchId();
            case BATCH_NAME -> row.getBatchName();
            case APPLICATION_STATUS -> row.getApplicationStatus();
            case APPLICATION_SOURCE -> row.getApplicationSource();
            case ARREARS_ITEM_NAMES -> row.getArrearsItemNames();
            case ARREARS_REASON_NAME -> row.getArrearsReasonName();
            case DECLARED_AMOUNT -> row.getDeclaredAmount();
            case CONFIRMED_AMOUNT -> row.getConfirmedAmount();
            case GIFT_ITEM_NAMES -> row.getGiftItemNames();
            case SUBSIDY_AMOUNT -> row.getSubsidyAmount();
            case APPLICATION_TIME -> formatTime(row.getApplicationTime());
            case COMPLETION_TIME -> formatTime(row.getCompletionTime());
        };
        return value == null ? "" : value;
    }

    private void writeHeader(
            SXSSFSheet sheet,
            List<StatisticsReportColumn> columns,
            CellStyle headerStyle
    ) {
        Row header = sheet.createRow(0);
        for (int index = 0; index < columns.size(); index++) {
            StatisticsReportColumn column = columns.get(index);
            Cell cell = header.createCell(index);
            cell.setCellValue(column.getTitle());
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(index, column.getWidth() * 256);
        }
    }

    private void writeExcelRow(
            SXSSFSheet sheet,
            int rowIndex,
            StatisticsReportRowVO source,
            List<StatisticsReportColumn> columns
    ) {
        Row row = sheet.createRow(rowIndex);
        for (int index = 0; index < columns.size(); index++) {
            Object value = columnValue(source, columns.get(index));
            Cell cell = row.createCell(index);
            if (value instanceof BigDecimal decimal) {
                cell.setCellValue(decimal.doubleValue());
            } else if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else {
                cell.setCellValue(String.valueOf(value));
            }
        }
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private StatisticsReportQueryDTO copyQuery(StatisticsReportQueryDTO source) {
        StatisticsReportQueryDTO target = new StatisticsReportQueryDTO();
        target.setBatchType(source.getBatchType());
        target.setBatchId(source.getBatchId());
        target.setCollegeId(source.getCollegeId());
        target.setMajorId(source.getMajorId());
        target.setGradeId(source.getGradeId());
        target.setClassId(source.getClassId());
        target.setApplicationType(source.getApplicationType());
        target.setApplicationStatus(source.getApplicationStatus());
        target.setFeeItemId(source.getFeeItemId());
        target.setApplicationStartTime(source.getApplicationStartTime());
        target.setApplicationEndTime(source.getApplicationEndTime());
        target.setColumns(source.getColumns());
        target.setSortBy(source.getSortBy());
        target.setSortDirection(source.getSortDirection());
        return target;
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }
}
