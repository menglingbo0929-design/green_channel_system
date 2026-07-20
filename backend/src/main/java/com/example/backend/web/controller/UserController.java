package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.mapper.UserRoleMapper;
import com.example.backend.model.domain.User;
import com.example.backend.model.dto.*;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.IUserService;
import com.example.backend.security.CurrentUserProvider;
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
    private final CurrentUserProvider currentUserProvider;

    /** 登录 */
    @PostMapping("login")
    public JsonResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getLoginName(), request.getPassword());
        if (user == null) {
            return JsonResponse.failure("用户名或密码错误");
        }
        List<String> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getLoginName(), roles);
        LoginResponse loginResponse = new LoginResponse(
                token, user.getId(), user.getLoginName());
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
    @PutMapping("password")
    public JsonResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req) {
        Long userId = currentUserProvider.getRequiredUser().getUserId();
        boolean ok = userService.changePassword(userId,
                req.getOldPassword(), req.getNewPassword());
        if (!ok) return JsonResponse.failure("旧密码错误");
        return JsonResponse.successMessage("密码修改成功");
    }
}
