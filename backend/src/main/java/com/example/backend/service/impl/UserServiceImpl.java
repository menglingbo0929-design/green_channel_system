package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.domain.User;
import com.example.backend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 *
 * 继承 MyBatis-Plus 的 ServiceImpl<UserMapper, User>：
 * 自动获得 baseMapper，提供 save、remove、update 等默认实现。
 *
 * 改动前后对比：
 * ┌────────────┬──────────────────────────────────────┐
 * │ 改动前     │ 改动后                                │
 * ├────────────┼──────────────────────────────────────┤
 * │ 用 @Autowired │ 用 @RequiredArgsConstructor 构造器注入 │
 * │ loginName+password │ 只查 loginName，password 用 BCrypt 校验 │
 * │ 明文查库   │ BCrypt.matches() 比对                  │
 * │ 不更新登录时间 │ 更新 lastLoginTime                     │
 * └────────────┴──────────────────────────────────────┘
 */
@Service  // 标记为 Service 层 Bean
@RequiredArgsConstructor  // Lombok：为 final 字段自动生成构造器，实现依赖注入
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /** 用户表 Mapper，用于数据库操作 */
    private final UserMapper userMapper;

    /**
     * 密码编码器（BCrypt 实现）
     * 由 SecurityConfig 中的 @Bean 方法创建，Spring 自动注入
     *
     * 两个核心方法：
     * - encode(明文)   → 生成 BCrypt 密文（注册时用）
     * - matches(明文, 密文) → 比对是否匹配（登录时用）
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录认证
     *
     * 执行流程：
     * 1. 用 LambdaQueryWrapper 构建查询条件（只按用户名查）
     * 2. 查数据库，用户不存在 → 返回 null
     * 3. 用 BCrypt 比对明文密码和数据库中密文 → 不匹配返回 null
     * 4. 更新 last_login_time 字段
     * 5. 返回 User 实体
     *
     * 为什么用 LambdaQueryWrapper 而不是写 SQL？
     * - 类型安全：用 User::getLoginName 而非字符串 "login_name"，改名时 IDE 自动重构
     * - 防 SQL 注入：MyBatis-Plus 自动用参数化查询
     *
     * 为什么先查用户再验密码，而不是把密码也作为查询条件？
     * - BCrypt 每次加密结果不同（盐值随机），无法直接用 SQL 比对
     * - 从 SQL 层面查不到用户才安全，而不是把密码暴露到 SQL 日志中
     *
     * @param loginName 用户名
     * @param password  明文密码
     * @return 认证成功返回 User，失败返回 null
     */
    @Override
    public User login(String loginName, String password) {
        // ── Step 1：按用户名查用户 ──
        // LambdaQueryWrapper 是 MyBatis-Plus 提供的类型安全查询构造器
        // eq(User::getLoginName, loginName) → WHERE login_name = ?
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLoginName, loginName);
        User user = userMapper.selectOne(queryWrapper);

        // ── Step 2：BCrypt 密码校验 ──
        // 为什么用 matches 而不是 encode 后比对字符串？
        // 因为 BCrypt 每次 encode 结果不同（随机盐），只能用 matches 方法
        // 注意：先判空再验密码，否则 user 为 null 时调用 user.getPassword() 会 NPE
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return null;  // 用户名不存在或密码错误，统一返回 null（防止用户名枚举攻击）
        }

        // ── Step 3：更新最后登录时间 ──
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);  // 只更新变化字段还是全量更新取决于 MyBatis-Plus 配置

        return user;
    }
}
