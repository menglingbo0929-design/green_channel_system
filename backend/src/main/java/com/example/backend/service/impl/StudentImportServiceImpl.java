package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.*;
import com.example.backend.model.domain.*;
import com.example.backend.model.dto.ImportResult;
import com.example.backend.service.StudentImportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentImportServiceImpl implements StudentImportService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final CollegeMapper collegeMapper;
    private final MajorMapper majorMapper;
    private final GradeMapper gradeMapper;
    private final ClassInfoMapper classInfoMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ImportResult importExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int success = 0, skipped = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum(); // 不含表头

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String studentNo  = getString(row, 0);
                    String name       = getString(row, 1);
                    String collegeCode = getString(row, 2);
                    String majorCode  = getString(row, 3);
                    String gradeCode  = getString(row, 4);
                    String classCode  = getString(row, 5);
                    String phone      = getString(row, 6);
                    int originLoan    = parseYesNo(getString(row, 7));
                    int campusLoan    = parseYesNo(getString(row, 8));

                    // 必填校验
                    if (studentNo.isEmpty() || name.isEmpty()) {
                        errors.add("第" + (i + 1) + "行：学号或姓名为空");
                        skipped++; continue;
                    }

                    // 学号重复
                    boolean exists = studentMapper.exists(
                            new LambdaQueryWrapper<Student>()
                                    .eq(Student::getStudentNo, studentNo)
                                    .eq(Student::getDeleted, 0));
                    if (exists) {
                        errors.add(studentNo + " " + name + "：学号已存在，跳过");
                        skipped++; continue;
                    }

                    // 查组织 ID
                    College college = findByCode(collegeMapper,
                            new LambdaQueryWrapper<College>().eq(College::getCollegeCode, collegeCode),
                            "学院编码不存在: " + collegeCode);
                    Major major = findByCode(majorMapper,
                            new LambdaQueryWrapper<Major>().eq(Major::getMajorCode, majorCode),
                            "专业编码不存在: " + majorCode);
                    Grade grade = findByCode(gradeMapper,
                            new LambdaQueryWrapper<Grade>().eq(Grade::getGradeCode, gradeCode),
                            "年级编码不存在: " + gradeCode);
                    if (college == null || major == null || grade == null) {
                        errors.add(studentNo + " " + name + "：" +
                                (college == null ? "学院编码不存在 " + collegeCode + " " : "") +
                                (major == null ? "专业编码不存在 " + majorCode + " " : "") +
                                (grade == null ? "年级编码不存在 " + gradeCode : ""));
                        skipped++; continue;
                    }

                    Long classId = null;
                    if (!classCode.isEmpty()) {
                        ClassInfo ci = classInfoMapper.selectOne(
                                new LambdaQueryWrapper<ClassInfo>()
                                        .eq(ClassInfo::getClassCode, classCode)
                                        .eq(ClassInfo::getDeleted, 0));
                        if (ci != null) classId = ci.getId();
                    }

                    // 创建用户
                    User user = new User();
                    user.setLoginName(studentNo);
                    user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
                    user.setRemark("Excel导入");
                    userMapper.insert(user);

                    // 绑定学生角色
                    UserRole ur = new UserRole();
                    ur.setUserId(user.getId());
                    ur.setRoleId(1L); // STUDENT
                    userRoleMapper.insert(ur);

                    // 创建学生
                    Student s = new Student();
                    s.setStudentNo(studentNo);
                    s.setStudentName(name);
                    s.setCollegeId(college.getId());
                    s.setMajorId(major.getId());
                    s.setGradeId(grade.getId());
                    s.setClassId(classId);
                    s.setPhone(phone);
                    s.setOriginLoan(originLoan);
                    s.setCampusLoan(campusLoan);
                    s.setUserId(user.getId());
                    s.setEnabled(1);
                    s.setInfoComplete(0);
                    studentMapper.insert(s);

                    success++;
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行：导入失败 — " + e.getMessage());
                    skipped++;
                }
            }
            return ImportResult.builder()
                    .total(totalRows)
                    .success(success)
                    .skipped(skipped)
                    .errors(errors)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Excel 解析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeTemplate(OutputStream out) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("学生导入模板");
            Row header = sheet.createRow(0);
            String[] titles = {"学号", "姓名", "学院编码", "专业编码", "年级编码", "班级编码", "电话", "生源地贷款", "校园地贷款"};
            for (int i = 0; i < titles.length; i++) {
                header.createCell(i).setCellValue(titles[i]);
            }
            // 示例行
            Row demo = sheet.createRow(1);
            demo.createCell(0).setCellValue("2024001");
            demo.createCell(1).setCellValue("张三");
            demo.createCell(2).setCellValue("CS");
            demo.createCell(3).setCellValue("CS001");
            demo.createCell(4).setCellValue("2024");
            demo.createCell(5).setCellValue("CS2401");
            demo.createCell(6).setCellValue("13800138000");
            demo.createCell(7).setCellValue("是");
            demo.createCell(8).setCellValue("否");
            wb.write(out);
        } catch (IOException e) {
            throw new RuntimeException("模板生成失败", e);
        }
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private int parseYesNo(String val) {
        return ("是".equals(val) || "1".equals(val) || "yes".equalsIgnoreCase(val)) ? 1 : 0;
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < 2; i++) {
            String v = getString(row, i);
            if (!v.isEmpty()) return false;
        }
        return true;
    }

    private <T> T findByCode(BaseMapper<T> mapper, LambdaQueryWrapper<T> wrapper, String errMsg) {
        return mapper.selectOne(wrapper);
    }
}
