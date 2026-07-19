package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.mapper.UserRoleMapper;
import com.example.backend.model.domain.User;
import com.example.backend.model.dto.LoginRequest;
import com.example.backend.model.dto.LoginResponse;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户控制器 —— 处理用户相关的 HTTP 请求
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    /** 用户角色 Mapper，登录时查用户角色塞进 JWT */
    private final UserRoleMapper userRoleMapper;

    /**
     * 用户登录接口
     *
     * 请求：POST /api/user/login
     * Body: { "loginName": "admin", "password": "123456" }
     *
     * 成功响应：
     * { "status": true, "message": "登录成功",
     *   "data": { "token": "eyJhbG...", "userId": 1, "loginName": "admin" } }
     *
     * Token 内部包含：userId、loginName、roles（如 "SCHOOL"）
     * 后续接口从 Token 解析角色做权限控制
     */
    @PostMapping("login")
    public JsonResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        // ── 1. 认证：查用户 + 验密码 ──
        User user = userService.login(request.getLoginName(), request.getPassword());
        if (user == null) {
            return JsonResponse.failure("用户名或密码错误");
        }

        // ── 2. 查用户角色（如 ["SCHOOL", "COUNSELOR"]） ──
        List<String> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());

        // ── 3. 生成 JWT（包含 userId + loginName + roles） ──
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getLoginName(), roles);

        // ── 4. 返回不含密码的安全响应 ──
        LoginResponse loginResponse = new LoginResponse(
                token, user.getId(), user.getLoginName());

        return JsonResponse.success(loginResponse, "登录成功");
    }
}
