package com.example.backend.security;

import com.example.backend.model.dto.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 当前用户提供者 —— 成员一对外暴露的核心接口
 *
 * 其他三个成员通过这个类获取当前登录用户信息，不需要自己解析 JWT。
 * 保证身份来源统一：所有模块从同一入口取用户信息，防止各模块
 * 从请求参数等不可信来源获取身份。
 *
 * 数据来源：JwtAuthenticationFilter 在请求进来时从 JWT 解析
 * 用户信息并写入 SecurityContextHolder，这里直接读取。
 *
 * 使用方式（其他成员的代码）：
 * <pre>
 *   LoginUser user = currentUserProvider.getRequiredUser();
 *   // user.getUserId()  → 当前用户 ID
 *   // user.getRoles()   → ["SCHOOL"]
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    /**
     * 获取当前请求的用户信息（必须已登录）
     *
     * 调用时机：Controller 或 Service 方法内，确保请求已经过 JwtAuthenticationFilter
     *
     * @return LoginUser 包含 userId、loginName、roles
     * @throws IllegalStateException 如果当前请求未认证
     */
    public LoginUser getRequiredUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("当前请求未认证，无法获取用户信息");
        }

        // loginName → getPrincipal()
        String loginName = (String) auth.getPrincipal();

        // userId → getDetails()
        Long userId = (Long) auth.getDetails();

        // roles → getAuthorities()，去掉 "ROLE_" 前缀还原角色编码
        //   Filter 里存的 "ROLE_SCHOOL" → 这里还原为 "SCHOOL"
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(s -> s.startsWith("ROLE_") ? s.substring(5) : s)
                .collect(Collectors.toList());

        return new LoginUser(userId, loginName, roles);
    }
}
