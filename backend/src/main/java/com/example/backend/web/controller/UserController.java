package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
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

/**
 * 用户控制器 —— 处理用户相关的 HTTP 请求
 *
 * 改动前后对比：
 * ┌──────────────┬────────────────────────────────────┐
 * │ 改动前       │ 改动后                              │
 * ├──────────────┼────────────────────────────────────┤
 * │ @Autowired   │ @RequiredArgsConstructor 构造器注入  │
 * │ 接收 User    │ 接收 LoginRequest（@Valid 校验）     │
 * │ 返回 User    │ 返回 LoginResponse（无密码）          │
 * │ 无 Token     │ 生成 JWT Token 返回前端              │
 * └──────────────┴────────────────────────────────────┘
 *
 * @RestController = @Controller + @ResponseBody
 * 所有方法的返回值自动序列化为 JSON，不走视图渲染
 */
@RestController
@RequestMapping("/api/user")  // 所有接口路径以 /api/user 开头
@RequiredArgsConstructor  // 构造器注入，比 @Autowired 字段注入更推荐
public class UserController {

    /** 用户 Service，处理业务逻辑 */
    private final IUserService userService;

    /** JWT 工具类，生成 Token */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户登录接口
     *
     * 请求：POST /api/user/login
     * Content-Type: application/json
     * Body: { "loginName": "admin", "password": "123456" }
     *
     * 成功响应：
     * {
     *   "status": true,
     *   "message": "登录成功",
     *   "data": {
     *     "token": "eyJhbGciOiJIUzI1NiJ9...",
     *     "userId": 1,
     *     "loginName": "admin"
     *   }
     * }
     *
     * 失败响应：
     * { "status": false, "message": "用户名或密码错误" }
     *
     * @param request 登录请求体（@Valid 自动校验 @NotBlank 注解）
     * @return JsonResponse 包含 Token 或错误信息
     */
    @PostMapping("login")  // 完整路径：POST /api/user/login
    public JsonResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // @Valid：触发 Jakarta Bean Validation，校验 LoginRequest 中的 @NotBlank 规则
        // 校验失败时 Spring 会自动返回 400，不会进入方法体
        // @RequestBody：将 HTTP 请求体的 JSON 反序列化为 LoginRequest 对象

        // ── 1. 调用 Service 层认证 ──
        User user = userService.login(request.getLoginName(), request.getPassword());

        // ── 2. 认证失败 → 返回错误 ──
        if (user == null) {
            return JsonResponse.failure("用户名或密码错误");
        }

        // ── 3. 认证成功 → 生成 JWT Token ──
        // Token 中包含 userId 和 loginName，有效期 24 小时（在 application.yml 配置）
        String token = jwtTokenProvider.generateToken(user.getId(), user.getLoginName());

        // ── 4. 构建安全响应：只返回 token、userId、loginName ──
        // 注意：不返回 password！不返回 deleted！不返回 remark！
        // LoginResponse 只有 3 个安全字段
        LoginResponse loginResponse = new LoginResponse(
                token, user.getId(), user.getLoginName());

        return JsonResponse.success(loginResponse, "登录成功");
    }
}
