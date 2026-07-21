package com.example.backend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 DTO（数据传输对象）
 *
 * 只返回前端需要的信息，绝不包含密码！
 * 对比修改前：直接返回整个 User 对象 → password 字段暴露在响应 JSON 中
 * 现在：只返回 token（JWT）、userId、loginName 三个安全字段
 */
@Data
@AllArgsConstructor  // Lombok 注解：自动生成包含所有字段的构造器
public class LoginResponse {

    /**
     * JWT 令牌字符串
     * 前端拿到后需要存起来（如 localStorage），
     * 后续每次请求都放在 Authorization 请求头里带来：
     * Authorization: Bearer <token>
     */
    @Schema(description = "JWT令牌")
    private String token;

    /** 用户 ID，前端可以用来显示"当前登录用户：xxx" */
    @Schema(description = "用户ID")
    private Long userId;

    /** 用户名，前端可以用来显示欢迎信息 */
    @Schema(description = "用户名")
    private String loginName;
}
