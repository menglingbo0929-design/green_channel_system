package com.example.backend.config;

import com.example.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类 —— 安全规则的"总控制台"
 *
 * 这个类决定：
 * ┌──────────────────────────────────────────────────────────┐
 * │ 1. 哪些接口公开（不需要登录就能访问）                       │
 * │ 2. 哪些接口需要认证（必须带有效 Token）                     │
 * │ 3. JWT Filter 插在哪个位置                                │
 * │ 4. 用不用 Session（JWT 模式不用）                          │
 * │ 5. 用不用 CSRF 防护（REST API 不需要）                     │
 * │ 6. 密码加密器（BCrypt）                                    │
 * └──────────────────────────────────────────────────────────┘
 *
 * @EnableWebSecurity 的作用：
 * 开启 Spring Security 的自动配置，并标记这个类是安全配置入口。
 * Spring Boot 会自动发现这个类，用里面的规则替换默认的安全配置。
 */
@Configuration  // 标记为配置类（会被 Spring 扫描并处理其中的 @Bean）
@EnableWebSecurity  // 开启 Web 安全配置
@RequiredArgsConstructor
public class SecurityConfig {

    /** 注入我们自定义的 JWT 过滤器 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Swagger 文档页面的路径白名单
     * 这些是 Swagger 2.x 的静态资源，必须公开才能看到 API 文档页面
     * ** 是通配符，匹配任意层级的路径
     */
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html",       // Swagger UI 主页面
            "/swagger-resources/**",   // Swagger 资源文件
            "/v2/api-docs/**",         // Swagger API 文档 JSON
            "/webjars/**"              // Swagger 依赖的前端静态资源
    };

    /**
     * 安全过滤器链 —— Spring Security 的核心配置
     *
     * 这个 Bean 定义了整个请求拦截规则。
     * 每个进来的 HTTP 请求，都会按这里配置的规则逐条检查。
     *
     * @param http Spring Security 的 HTTP 安全构建器，链式配置
     * @return 构建好的过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ═══ 关闭 CSRF 防护 ═══
                // CSRF（跨站请求伪造）：攻击者诱导用户在已登录的网站执行非本意操作
                // 传统 Web 用 Cookie+Session 容易受 CSRF 攻击
                // 但我们是 REST API + JWT：Token 存在请求头里，不靠 Cookie，
                // 攻击者无法构造带正确 Token 的请求，所以 CSRF 防护可以放心关掉
                .csrf(csrf -> csrf.disable())

                // ═══ 会话管理：无状态 ═══
                // STATELESS：Spring Security 不创建、不使用 HttpSession
                // 每次请求独立验证，靠 JWT Token 识别用户
                // 好处：服务端不存任何用户状态，天然支持集群部署
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ═══ URL 访问权限规则 ═══
                // 规则从上到下匹配，一旦匹配就停止
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // permitAll() = 放行，不管带没带 Token 都能访问
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()  // Swagger 页面
                        .requestMatchers("/api/user/login").permitAll()   // 密码登录
                        .requestMatchers("/api/green-channel/**").permitAll()
                        .requestMatchers("/api/user/login-by-code").permitAll()  // 验证码登录
                        .requestMatchers("/api/verification-code/send").permitAll()  // 发送验证码
                        // authenticated() = 必须带有效 Token 才能访问
                        .anyRequest().authenticated()
                )

                // ═══ 插入自定义 JWT 过滤器 ═══
                // addFilterBefore(filter, beforeFilter)：
                // 把我们的 JwtAuthenticationFilter 插在 UsernamePasswordAuthenticationFilter 之前
                //
                // UsernamePasswordAuthenticationFilter 是 Spring Security 自带的
                // 表单登录过滤器，处理 /login 的 POST 请求。
                // 我们不用表单登录，但需要确保 JWT Filter 在它之前执行，
                // 这样 JWT 解析出的认证信息能被后面的过滤器使用。
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();  // 构建并返回过滤器链
    }

    /**
     * 密码编码器 Bean
     *
     * BCryptPasswordEncoder 的特点：
     * 1. 单向加密：只能加密不能解密。验证时用 matches(明文, 密文) 比对
     * 2. 内置盐值：每次加密自动生成随机盐，同样的密码每次加密结果不同
     * 3. 故意慢速：单次加密约 0.1 秒，暴力破解成本极高
     * 4. 密文格式：$2a$10$...  10 = 加密强度（迭代 2^10 次）
     *
     * 注册这个 Bean 后，任何地方注入 PasswordEncoder 都能直接使用
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
