package com.example.backend.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 密码迁移器 —— 启动时自动将数据库中的明文密码加密为 BCrypt
 *
 * 背景：项目最初用明文存密码，引入 Spring Security 后需要 BCrypt 加密。
 * 这个类在应用启动时执行一次扫描，把还没加密的密码自动转成 BCrypt。
 *
 * 实现 CommandLineRunner 接口：Spring Boot 启动完成后自动调用 run() 方法
 * 执行时机：所有 Bean 初始化完成后，接收请求之前
 *
 * ⚠️ 密码全部迁移完成后可以删除这个文件
 */
@Slf4j
@Component  // 让 Spring 扫描并管理这个 Bean
@ConditionalOnProperty(name = "app.password-migration.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PasswordMigrationRunner implements CommandLineRunner {

    private final UserMapper userMapper;         // 操作 user 表
    private final PasswordEncoder passwordEncoder; // BCrypt 加密器

    /**
     * BCrypt 密文的固定前缀
     * 所有 BCrypt 加密结果都以 "$2a$" 开头，后面跟着加密强度和盐值
     */
    private static final String BCRYPT_PREFIX = "$2a$";

    /**
     * Spring Boot 启动后自动执行
     *
     * 逻辑：
     * 1. 查出所有用户
     * 2. 逐个检查密码是否已经是 BCrypt 格式
     * 3. 不是的就加密并更新回数据库
     *
     * @param args 命令行参数（用不到）
     */
    @Override
    public void run(String... args) {
        // 查出所有用户（包括逻辑删除的，所以用 selectList 不加条件）
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());

        int migratedCount = 0;  // 计数器：迁移了多少个用户

        for (User user : users) {
            String password = user.getPassword();

            // 如果密码为空，或者是 BCrypt 格式，跳过
            if (password == null || password.startsWith(BCRYPT_PREFIX)) {
                continue;
            }

            // 明文密码 → BCrypt 加密 → 更新回数据库
            log.info("正在迁移用户 [{}] 的密码...", user.getLoginName());
            user.setPassword(passwordEncoder.encode(password));
            userMapper.updateById(user);
            migratedCount++;
        }

        if (migratedCount > 0) {
            log.info("✅ 密码迁移完成，共加密 {} 个用户的密码", migratedCount);
        } else {
            log.info("✅ 所有用户密码已是 BCrypt 格式，无需迁移");
        }
    }
}
