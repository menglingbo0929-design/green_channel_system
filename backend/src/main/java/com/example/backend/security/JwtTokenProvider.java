package com.example.backend.security;

import com.example.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 令牌工具类 —— 负责 Token 的生成、验证、解析
 *
 * 核心概念：
 * ┌──────────────────────────────────────────────────────┐
 * │  JWT 结构：Header.Payload.Signature                  │
 * │  Header：  {"alg":"HS256"}          — 签名算法        │
 * │  Payload： {"sub":"admin","userId":1,"exp":...} — 存的数据 │
 * │  Signature：加密签名                  — 防篡改         │
 * └──────────────────────────────────────────────────────┘
 *
 * Payload 是 Base64 编码，不是加密！任何人都能解码看到内容。
 * 所以千万不要在 Token 里放密码等敏感信息。
 * 安全性靠 Signature 保证：篡改 Payload → 签名不匹配 → Token 失效。
 *
 * HS256 = HMAC-SHA256，对称加密，签发和验证用同一把密钥
 */
@Slf4j  // Lombok 注解：自动生成 log 对象，可以用 log.info()、log.warn() 打日志
@Component  // 注册为 Spring Bean
@RequiredArgsConstructor  // Lombok：为 final 字段生成构造器，实现构造器注入
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;  // 注入配置（secret 和 expiration）
    private SecretKey secretKey;  // 签名密钥对象，在 init() 中初始化

    /**
     * 初始化方法
     * @PostConstruct 保证在 Bean 创建完成后、接受请求之前执行
     *
     * 流程：Base64 字符串 → 字节数组 → SecretKey 对象
     * 只执行一次，后续复用 secretKey
     */
    @PostConstruct
    public void init() {
        // 1. 用 Base64 解码器把配置中的字符串解码成字节数组
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        // 2. 用字节数组创建 HMAC-SHA256 密钥对象
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     *
     * @param userId   用户 ID，存入 Payload 的 userId 字段
     * @param loginName 用户名，存入 Payload 的 sub（subject）字段
     * @return JWT 字符串，如 "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiI..."
     *
     * Token 包含：
     * - sub: 用户名（JWT 标准字段）
     * - userId: 自定义字段
     * - iat: 签发时间（JWT 标准字段）
     * - exp: 过期时间（JWT 标准字段，过了这个时间 Token 自动失效）
     */
    public String generateToken(Long userId, String loginName) {
        Date now = new Date();
        // 过期时间 = 当前时间 + 配置的过期时长
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()           // 建造者模式，链式调用构建 Token
                .subject(loginName)     // 设置主题（用户名）
                .claim("userId", userId) // 设置自定义字段（用户ID）
                .issuedAt(now)          // 签发时间
                .expiration(expiration) // 过期时间
                .signWith(secretKey)    // 用密钥签名
                .compact();             // 压缩成最终的 JWT 字符串
    }

    /**
     * 从 Token 中提取用户 ID
     * 在 JwtAuthenticationFilter 中调用，用于识别当前请求是谁发的
     */
    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    /**
     * 从 Token 中提取用户名
     * getSubject() 读取的是 JWT 标准字段 sub
     */
    public String getLoginNameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 验证 Token 是否有效
     *
     * 验证逻辑（parseClaims 自动完成）：
     * 1. 签名是否正确（有没有被篡改）
     * 2. Token 是否过期
     * 3. Token 格式是否正确
     *
     * 任何一项不通过 → catch JwtException → 返回 false
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);  // 尝试解析，解析成功 = Token 有效
            return true;
        } catch (JwtException e) {
            log.warn("JWT 验证失败: {}", e.getMessage());  // 记录警告日志，方便排查
            return false;
        }
    }

    /**
     * 内部方法：解析 Token，提取 Payload（Claims）
     *
     * jjwt 0.12.x 的 API 用法：
     * Jwts.parser()          → 创建解析器建造者
     *   .verifyWith(key)     → 设置验证签名用的密钥
     *   .build()            → 构建解析器
     *   .parseSignedClaims(token) → 解析 Token，同时验证签名
     *   .getPayload()       → 获取 Payload（Claims 对象）
     *
     * 如果签名不对或 Token 过期，parseSignedClaims 会抛异常
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)       // 指定验证密钥
                .build()                      // 构建解析器
                .parseSignedClaims(token)     // 解析并验签
                .getPayload();               // 获取 Payload 数据
    }
}
