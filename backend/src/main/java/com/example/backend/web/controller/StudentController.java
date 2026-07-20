package com.example.backend.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.JsonResponse;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentMapper studentMapper;

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
        return JsonResponse.successMessage("新增成功");
    }

    /** 编辑 */
    @PutMapping("{id}")
    public JsonResponse<Void> update(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        student.setUpdateTime(LocalDateTime.now());
        studentMapper.updateById(student);
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
}
