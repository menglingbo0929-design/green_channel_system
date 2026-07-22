package com.example.backend.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.JsonResponse;
import com.example.backend.config.VerificationCodeStore;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.mapper.UserRoleMapper;
import com.example.backend.mapper.UserCollegeScopeMapper;
import com.example.backend.model.domain.Student;
import com.example.backend.model.domain.User;
import com.example.backend.model.dto.*;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.IUserService;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.StudentUserMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRoleMapper userRoleMapper;
    private final UserCollegeScopeMapper userCollegeScopeMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final VerificationCodeStore codeStore;
    private final ICurrentUserProvider currentUserProvider;
    private final StudentUserMappingService studentUserMappings;

    /** 登录 */
    @PostMapping("login")
    public JsonResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getLoginName(), request.getPassword());
        if (user == null) {
            return JsonResponse.failure("用户名或密码错误");
        }
        List<String> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());

        // 查询该用户是否关联学生，填充 studentId 和 collegeId 到 JWT
        Long studentId = null;
        Long collegeId = null;
        Student student = studentUserMappings.findActiveStudentByUserId(user.getId());
        if (student != null) {
            studentId = student.getId();
            collegeId = student.getCollegeId();
        } else if (roles.contains("COLLEGE")) {
            collegeId = userCollegeScopeMapper.findCollegeIdByUserId(user.getId());
            if (collegeId == null) return JsonResponse.failure("学院账号尚未配置所属学院范围");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getLoginName(), roles, studentId, collegeId);
        LoginResponse loginResponse = new LoginResponse(
                token, user.getId(), user.getLoginName());
        return JsonResponse.success(loginResponse, "登录成功");
    }

    /** 短信验证码登录 */
    @PostMapping("login-by-code")
    public JsonResponse<LoginResponse> loginByCode(@Valid @RequestBody LoginByCodeRequest request) {
        if (!codeStore.verify(request.getPhone(), request.getCode())) {
            return JsonResponse.failure("验证码错误或已过期");
        }

        // 先根据手机号找学生
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getPhone, request.getPhone())
                        .eq(Student::getDeleted, 0));
        if (student == null) {
            return JsonResponse.failure("该手机号未关联学生");
        }

        // 通过学生的 user_id 获取登录用户
        if (student.getUserId() == null) {
            return JsonResponse.failure("该学生尚未创建登录账号");
        }
        User user = userMapper.selectById(student.getUserId());
        if (user == null || user.getDeleted() != 0) {
            return JsonResponse.failure("账号不存在或已停用");
        }

        List<String> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getLoginName(), roles,
                student.getId(), student.getCollegeId());

        LoginResponse loginResponse = new LoginResponse(token, user.getId(), user.getLoginName());
        return JsonResponse.success(loginResponse, "登录成功");
    }

    /** 用户列表 */
    @GetMapping("list")
    public JsonResponse<List<UserVO>> list() {
        return JsonResponse.success(userService.listUsers());
    }

    /** 新增用户 */
    @PostMapping
    public JsonResponse<Void> create(@Valid @RequestBody CreateUserRequest request) {
        userService.createUser(
                request.getLoginName(), request.getPassword(),
                request.getRemark(), request.getRoleIds());
        return JsonResponse.successMessage("新增成功");
    }

    /** 编辑用户 */
    @PutMapping("{id}")
    public JsonResponse<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateUserRequest request) {
        userService.updateUser(
                id, request.getLoginName(), request.getRemark(), request.getRoleIds());
        return JsonResponse.successMessage("更新成功");
    }

    /** 切换启用/停用 */
    @PutMapping("{id}/status")
    public JsonResponse<Void> toggleStatus(@PathVariable Long id) {
        userService.toggleStatus(id);
        return JsonResponse.successMessage("操作成功");
    }

    /** 修改当前用户的密码 */
    /** Returns the trusted identity decoded from the request JWT. */
    @GetMapping("current")
    public JsonResponse<LoginUser> current() {
        return JsonResponse.success(currentUserProvider.getRequiredUser());
    }

    @PutMapping("password")
    public JsonResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req) {
        Long userId = currentUserProvider.getRequiredUser().getUserId();
        boolean ok = userService.changePassword(userId,
                req.getOldPassword(), req.getNewPassword());
        if (!ok) return JsonResponse.failure("旧密码错误");
        return JsonResponse.successMessage("密码修改成功");
    }
}
