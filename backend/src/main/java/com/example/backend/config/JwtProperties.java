package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性类
 *
 * 负责读取 application.yml 中的 jwt 配置段：
 * <pre>
 * jwt:
 *   secret: xxxxx    → 绑定到 secret 字段
 *   expiration: 86400000 → 绑定到 expiration 字段
 * </pre>
 *
 * 为什么单独抽一个类而不是直接 @Value？
 * - @ConfigurationProperties 支持类型安全、IDE 自动补全
 * - 多个配置项聚合成一个对象，注入更简洁
 * - 可以加 @Validated 做配置校验（如 secret 不能为空）
 */
@Data
@Component  // 注册为 Spring Bean，让其他类可以注入
@ConfigurationProperties(prefix = "jwt")  // 绑定 application.yml 中以 jwt 开头的配置
public class JwtProperties {

    /**
     * JWT 签名密钥（Base64 编码）
     * 要求：解码后至少 256 bits（32 字节），否则 HS256 算法会报错
     * 生成方式：openssl rand -base64 32
     *
     * 这个密钥绝对不能泄露！任何人拿到它都能伪造 Token
     */
    private String secret;

    /**
     * Token 过期时间，单位：毫秒
     * 86400000ms = 24 小时
     * 过期后前端需要重新登录获取新 Token
     */
    private long expiration;
}
