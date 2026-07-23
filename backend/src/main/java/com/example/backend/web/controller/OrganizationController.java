package com.example.backend.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.JsonResponse;
import com.example.backend.mapper.*;
import com.example.backend.model.domain.*;
import com.example.backend.security.ICurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织结构管理 —— 学院、专业、年级、班级的增删改查
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrganizationController {

    private final CollegeMapper collegeMapper;
    private final MajorMapper majorMapper;
    private final GradeMapper gradeMapper;
    private final ClassInfoMapper classInfoMapper;
    private final UserCollegeScopeMapper userCollegeScopeMapper;
    private final CounselorStudentMapper counselorStudentMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ICurrentUserProvider currentUsers;

    // ==================== 学院 College ====================

    @GetMapping("college/list")
    public JsonResponse<List<College>> listColleges() {
        requireSchool();
        return JsonResponse.success(collegeMapper.selectList(null));
    }

    @PostMapping("college")
    public JsonResponse<Void> createCollege(@RequestBody College college) {
        requireSchool();
        college.setCreateTime(LocalDateTime.now());
        college.setUpdateTime(LocalDateTime.now());
        collegeMapper.insert(college);
        return JsonResponse.successMessage("新增成功");
    }

    @PutMapping("college/{id}")
    public JsonResponse<Void> updateCollege(@PathVariable Long id, @RequestBody College college) {
        requireSchool();
        college.setId(id);
        college.setUpdateTime(LocalDateTime.now());
        collegeMapper.updateById(college);
        return JsonResponse.successMessage("更新成功");
    }

    @PutMapping("college/{id}/status")
    public JsonResponse<Void> toggleCollege(@PathVariable Long id) {
        requireSchool();
        College c = collegeMapper.selectById(id);
        if (c != null) { c.setEnabled(c.getEnabled() == 1 ? 0 : 1); collegeMapper.updateById(c); }
        return JsonResponse.successMessage("操作成功");
    }

    // ==================== 专业 Major ====================

    @GetMapping("major/list")
    public JsonResponse<List<Major>> listMajors(@RequestParam(required = false) Long collegeId) {
        requireSchool();
        LambdaQueryWrapper<Major> q = new LambdaQueryWrapper<>();
        if (collegeId != null) q.eq(Major::getCollegeId, collegeId);
        return JsonResponse.success(majorMapper.selectList(q));
    }

    @PostMapping("major")
    public JsonResponse<Void> createMajor(@RequestBody Major major) {
        requireSchool();
        major.setCreateTime(LocalDateTime.now());
        major.setUpdateTime(LocalDateTime.now());
        majorMapper.insert(major);
        return JsonResponse.successMessage("新增成功");
    }

    @PutMapping("major/{id}")
    public JsonResponse<Void> updateMajor(@PathVariable Long id, @RequestBody Major major) {
        requireSchool();
        major.setId(id);
        major.setUpdateTime(LocalDateTime.now());
        majorMapper.updateById(major);
        return JsonResponse.successMessage("更新成功");
    }

    @PutMapping("major/{id}/status")
    public JsonResponse<Void> toggleMajor(@PathVariable Long id) {
        requireSchool();
        Major m = majorMapper.selectById(id);
        if (m != null) { m.setEnabled(m.getEnabled() == 1 ? 0 : 1); majorMapper.updateById(m); }
        return JsonResponse.successMessage("操作成功");
    }

    // ==================== 年级 Grade ====================

    @GetMapping("grade/list")
    public JsonResponse<List<Grade>> listGrades() {
        requireSchool();
        return JsonResponse.success(gradeMapper.selectList(null));
    }

    @PostMapping("grade")
    public JsonResponse<Void> createGrade(@RequestBody Grade grade) {
        requireSchool();
        grade.setCreateTime(LocalDateTime.now());
        grade.setUpdateTime(LocalDateTime.now());
        gradeMapper.insert(grade);
        return JsonResponse.successMessage("新增成功");
    }

    @PutMapping("grade/{id}")
    public JsonResponse<Void> updateGrade(@PathVariable Long id, @RequestBody Grade grade) {
        requireSchool();
        grade.setId(id);
        grade.setUpdateTime(LocalDateTime.now());
        gradeMapper.updateById(grade);
        return JsonResponse.successMessage("更新成功");
    }

    @PutMapping("grade/{id}/status")
    public JsonResponse<Void> toggleGrade(@PathVariable Long id) {
        requireSchool();
        Grade g = gradeMapper.selectById(id);
        if (g != null) { g.setEnabled(g.getEnabled() == 1 ? 0 : 1); gradeMapper.updateById(g); }
        return JsonResponse.successMessage("操作成功");
    }

    // ==================== 班级 ClassInfo ====================

    @GetMapping("class/list")
    public JsonResponse<List<ClassInfo>> listClasses(@RequestParam(required = false) Long collegeId,
                                                     @RequestParam(required = false) Long majorId,
                                                     @RequestParam(required = false) Long gradeId) {
        requireSchool();
        LambdaQueryWrapper<ClassInfo> q = new LambdaQueryWrapper<>();
        if (collegeId != null) q.eq(ClassInfo::getCollegeId, collegeId);
        if (majorId != null) q.eq(ClassInfo::getMajorId, majorId);
        if (gradeId != null) q.eq(ClassInfo::getGradeId, gradeId);
        return JsonResponse.success(classInfoMapper.selectList(q));
    }

    @PostMapping("class")
    public JsonResponse<Void> createClass(@RequestBody ClassInfo classInfo) {
        requireSchool();
        classInfo.setCreateTime(LocalDateTime.now());
        classInfo.setUpdateTime(LocalDateTime.now());
        classInfoMapper.insert(classInfo);
        return JsonResponse.successMessage("新增成功");
    }

    @PutMapping("class/{id}")
    public JsonResponse<Void> updateClass(@PathVariable Long id, @RequestBody ClassInfo classInfo) {
        requireSchool();
        classInfo.setId(id);
        classInfo.setUpdateTime(LocalDateTime.now());
        classInfoMapper.updateById(classInfo);
        return JsonResponse.successMessage("更新成功");
    }

    @PutMapping("class/{id}/status")
    public JsonResponse<Void> toggleClass(@PathVariable Long id) {
        requireSchool();
        ClassInfo c = classInfoMapper.selectById(id);
        if (c != null) { c.setEnabled(c.getEnabled() == 1 ? 0 : 1); classInfoMapper.updateById(c); }
        return JsonResponse.successMessage("操作成功");
    }

    // ==================== 学院用户范围 ====================

    @GetMapping("college-scope/list")
    public JsonResponse<List<UserCollegeScope>> listCollegeScopes() {
        return JsonResponse.success(userCollegeScopeMapper.selectList(null));
    }

    @PostMapping("college-scope")
    public JsonResponse<Void> addCollegeScope(@RequestParam Long userId, @RequestParam Long collegeId) {
        boolean exists = userCollegeScopeMapper.exists(
                new LambdaQueryWrapper<UserCollegeScope>()
                        .eq(UserCollegeScope::getUserId, userId)
                        .eq(UserCollegeScope::getCollegeId, collegeId));
        if (!exists) {
            UserCollegeScope scope = new UserCollegeScope();
            scope.setUserId(userId);
            scope.setCollegeId(collegeId);
            userCollegeScopeMapper.insert(scope);
        }
        return JsonResponse.successMessage("关联成功");
    }

    @DeleteMapping("college-scope/{id}")
    public JsonResponse<Void> removeCollegeScope(@PathVariable Long id) {
        userCollegeScopeMapper.deleteById(id);
        return JsonResponse.successMessage("已解除");
    }

    // ==================== 辅导员-学生关联 ====================

    @GetMapping("counselor-scope/list")
    public JsonResponse<List<CounselorStudent>> listCounselorScopes() {
        return JsonResponse.success(counselorStudentMapper.selectList(null));
    }

    @PostMapping("counselor-scope")
    public JsonResponse<Void> addCounselorScope(@RequestParam Long counselorUserId, @RequestParam Long studentId) {
        boolean exists = counselorStudentMapper.exists(
                new LambdaQueryWrapper<CounselorStudent>()
                        .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                        .eq(CounselorStudent::getStudentId, studentId));
        if (!exists) {
            CounselorStudent cs = new CounselorStudent();
            cs.setCounselorUserId(counselorUserId);
            cs.setStudentId(studentId);
            counselorStudentMapper.insert(cs);
        }
        return JsonResponse.successMessage("关联成功");
    }

    @DeleteMapping("counselor-scope/{id}")
    public JsonResponse<Void> removeCounselorScope(@PathVariable Long id) {
        counselorStudentMapper.deleteById(id);
        return JsonResponse.successMessage("已解除");
    }

    /** 查询所有辅导员（供学生选辅导员时下拉） */
    @GetMapping("counselors")
    public JsonResponse<List<User>> listCounselors() {
        List<Long> ids = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRoleRelation>().eq(UserRoleRelation::getRoleId, 2L))
                .stream().map(UserRoleRelation::getUserId).toList();
        if (ids.isEmpty()) return JsonResponse.success(List.of());
        return JsonResponse.success(userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getId, ids).eq(User::getDeleted, 0L)));
    }

    private void requireSchool() {
        var user = currentUsers.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new SecurityException("仅学校管理员可维护组织基础数据");
        }
    }
}
