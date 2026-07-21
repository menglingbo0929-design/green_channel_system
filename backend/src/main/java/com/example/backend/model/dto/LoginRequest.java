package com.example.backend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO（数据传输对象）
 *
 * 只用于接收前端传来的登录表单数据，不和数据库直接交互。
 * 与 User 实体的区别：
 * - User 是数据库表的映射，包含 id、password、gmtCreated 等所有字段
 * - LoginRequest 只包含登录需要的两个字段，更轻量、更安全
 */
@Data  // Lombok 注解：自动生成 getter、setter、toString、equals、hashCode
public class LoginRequest {

    /**
     * 用户名
     * @NotBlank 确保字符串不为 null、不为空串、不全为空格
     *           如果校验失败，Spring 会自动返回 400 错误，不会进入 Controller
     */
    @Schema(description = "Username", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String loginName;

    /**
     * 密码（明文）
     * 前端传明文，后端用 BCrypt 加密后和数据库密文比对
     */
    @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;
}
