package com.example.backend.security;

import com.example.backend.common.JsonResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器 —— 整个 JWT 认证的核心拦截器
 *
 * 继承 OncePerRequestFilter：保证每个请求只被这个 Filter 处理一次
 * （有些 Filter 可能因为请求转发被多次触发，OncePerRequestFilter 避免了这个问题）
 *
 * 执行时机：
 * 请求 → JwtAuthenticationFilter → 其他安全过滤器 → Controller
 *
 * 处理逻辑：
 * 1. 从请求头 Authorization 中提取 Token
 * 2. 没带 Token → 放行（交给 SecurityConfig 的规则决定是否拒绝）
 * 3. 带了 Token 但无效/过期 → 直接返回 401
 * 4. 带了有效 Token → 解析出用户信息 → 写入 SecurityContext → 放行
 *
 * SecurityContext 是什么？
 * Spring Security 的"当前用户上下文"，类似一个全局变量，
 * 存储当前请求的认证信息。写入后，Controller 里随时可以获取当前用户是谁。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** HTTP 头中 Token 的前缀，"Bearer " 后面才是真正的 Token */
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;  // 注入 JWT 工具类
    private final ObjectMapper objectMapper;           // 注入 Jackson JSON 序列化器（用于写错误响应）

    /**
     * 核心方法：对每个请求执行过滤逻辑
     *
     * @param request   HTTP 请求
     * @param response  HTTP 响应
     * @param filterChain 过滤器链，调用 doFilter 表示放行给下一个过滤器
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ═══ 第一步：从请求头中提取 Token ═══
        String token = extractToken(request);

        // ═══ 第二步：没带 Token → 直接放行 ═══
        // 不在这里拒绝——如果请求的是 /api/user/login（公开接口），允许访问
        // 如果请求的是需要认证的接口，后面 SecurityConfig 会拦截并返回 403
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ═══ 第三步：Token 无效或过期 → 返回 401 ═══
        if (!jwtTokenProvider.validateToken(token)) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token无效或已过期");
            return;
        }

        // ═══ 第四步：Token 有效 → 解析用户信息，写入 SecurityContext ═══
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        String loginName = jwtTokenProvider.getLoginNameFromToken(token);
        List<String> roles = jwtTokenProvider.getRolesFromToken(token);

        // 将角色转为 Spring Security 的权限对象
        // ROLE_ 前缀是 Spring Security 的约定，如 ROLE_SCHOOL
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());

        // UsernamePasswordAuthenticationToken 是 Spring Security 的认证对象
        // 参数：（用户名, 密码, 权限列表）—— 这里把角色存入 authorities，
        //       CurrentUserProvider 可以通过 getAuthorities() 拿到
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        loginName, null, authorities);
        // 把 userId 存入 details，方便后续 Controller 获取
        authentication.setDetails(userId);

        // 写入 SecurityContext：这行代码执行后，Spring Security 认为"当前用户已登录"
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ═══ 第五步：放行 ═══
        filterChain.doFilter(request, response);
    }

    /**
     * 从 HTTP 请求头中提取 Bearer Token
     *
     * 请求头格式：
     * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiI...
     *
     * @return 提取到的 Token 字符串（不含 "Bearer " 前缀），没找到则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        // Authorization 是 HTTP 标准请求头，存放认证凭据
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        // hasText：字符串不为 null、不为空串、不全为空格
        // startsWith：必须以 "Bearer " 开头
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            // substring(7)：跳过 "Bearer " 这 7 个字符，拿到纯 Token
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 向前端返回 JSON 格式的错误响应
     *
     * 为什么要手动写 JSON？
     * Filter 在 Spring MVC 之前执行，此时 Controller 的异常处理器还没生效，
     * 所以不能靠抛异常来返回错误，必须直接操作 HttpServletResponse。
     *
     * @param response HTTP 响应对象
     * @param status   HTTP 状态码（如 401）
     * @param message  错误消息
     */
    private void sendErrorResponse(HttpServletResponse response,
                                   int status, String message) throws IOException {
        response.setStatus(status);                              // 设置 HTTP 状态码 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // 内容类型：JSON
        response.setCharacterEncoding("UTF-8");                   // 编码：UTF-8

        // 用项目的 JsonResponse 格式返回错误，和 Controller 返回的格式保持一致
        JsonResponse<Void> errorResponse = JsonResponse.failure(message);
        errorResponse.setCode(status);

        // 用 Jackson 的 ObjectMapper 把 Java 对象序列化成 JSON 字符串写入响应体
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
