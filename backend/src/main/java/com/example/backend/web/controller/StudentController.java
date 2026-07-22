package com.example.backend.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.JsonResponse;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.Student;
import com.example.backend.model.dto.ImportResult;
import com.example.backend.service.StudentImportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentMapper studentMapper;
    private final StudentImportService importService;
    private final ICurrentUserProvider currentUserProvider;
    private final CounselorStudentService counselorStudents;

    /** 列表查询 + 多条件筛选 */
    @GetMapping("list")
    public JsonResponse<List<Student>> list(
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long classId) {

        LambdaQueryWrapper<Student> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(studentNo)) q.like(Student::getStudentNo, studentNo);
        if (StringUtils.hasText(studentName)) q.like(Student::getStudentName, studentName);
        if (collegeId != null) q.eq(Student::getCollegeId, collegeId);
        if (majorId != null) q.eq(Student::getMajorId, majorId);
        if (gradeId != null) q.eq(Student::getGradeId, gradeId);
        if (classId != null) q.eq(Student::getClassId, classId);
        q.orderByDesc(Student::getId);

        return JsonResponse.success(studentMapper.selectList(q));
    }

    /** 新增 */
    @PostMapping
    public JsonResponse<Void> create(@RequestBody Student student) {
        student.setCreateTime(LocalDateTime.now());
        student.setUpdateTime(LocalDateTime.now());
        studentMapper.insert(student);
        if (student.getCounselorId() != null) {
            counselorStudents.assign(student.getCounselorId(), student.getId());
        }
        return JsonResponse.successMessage("新增成功");
    }

    /** 编辑 */
    @PutMapping("{id}")
    public JsonResponse<Void> update(@PathVariable Long id, @RequestBody Student student) {
        Student existing = studentMapper.selectById(id);
        if (existing == null) return JsonResponse.failure("Student does not exist");
        Long previousCounselorId = existing.getCounselorId();
        student.setId(id);
        student.setUpdateTime(LocalDateTime.now());
        studentMapper.updateById(student);
        if (student.getCounselorId() != null && !student.getCounselorId().equals(previousCounselorId)) {
            if (previousCounselorId != null) counselorStudents.remove(previousCounselorId, id);
            counselorStudents.assign(student.getCounselorId(), id);
        }
        return JsonResponse.successMessage("更新成功");
    }

    /** 切换启用/停用 */
    @PutMapping("{id}/status")
    public JsonResponse<Void> toggleStatus(@PathVariable Long id) {
        Student s = studentMapper.selectById(id);
        if (s != null) {
            s.setEnabled(s.getEnabled() == 1 ? 0 : 1);
            studentMapper.updateById(s);
        }
        return JsonResponse.successMessage("操作成功");
    }

    /** Excel 导入 */
    @PostMapping("import")
    public JsonResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return JsonResponse.failure("请选择文件");
        }
        return JsonResponse.success(importService.importExcel(file), "导入完成");
    }

    /** 学生查看本人资料 */
    @GetMapping("profile")
    public JsonResponse<Student> getProfile() {
        Long studentId = currentUserProvider.getRequiredUser().getStudentId();
        if (studentId == null) return JsonResponse.failure("当前用户不是学生");
        Student s = studentMapper.selectById(studentId);
        if (s == null || s.getDeleted() != 0) return JsonResponse.failure("学生不存在");
        return JsonResponse.success(s);
    }

    /** 学生完善本人资料（禁止修改学号和组织） */
    @PutMapping("profile")
    public JsonResponse<Void> updateProfile(@RequestBody Map<String, Object> body) {
        Long studentId = currentUserProvider.getRequiredUser().getStudentId();
        if (studentId == null) return JsonResponse.failure("当前用户不是学生");
        Student s = studentMapper.selectById(studentId);
        if (s == null || s.getDeleted() != 0) return JsonResponse.failure("学生不存在");

        if (body.containsKey("phone")) s.setPhone((String) body.get("phone"));
        if (body.containsKey("originLoan")) s.setOriginLoan((Integer) body.get("originLoan"));
        if (body.containsKey("campusLoan")) s.setCampusLoan((Integer) body.get("campusLoan"));
        if (body.containsKey("difficultyLevel")) s.setDifficultyLevel((String) body.get("difficultyLevel"));
        s.setInfoComplete(1);
        s.setUpdateTime(LocalDateTime.now());
        studentMapper.updateById(s);
        return JsonResponse.successMessage("保存成功");
    }

    /** 下载导入模板 */
    @GetMapping("template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        String filename = URLEncoder.encode("学生导入模板.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        importService.writeTemplate(response.getOutputStream());
    }
}
