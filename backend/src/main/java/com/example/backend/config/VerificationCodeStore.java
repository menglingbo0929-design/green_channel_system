package com.example.backend.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码内存存储 —— 生产环境应替换为 Redis
 *
 * 每个手机号限制 60s 内只能发一条，验证码 5 分钟有效。
 */
@Component
public class VerificationCodeStore {

    /** 手机号 → 最后发送时间戳 */
    private final Map<String, Long> lastSendTime = new ConcurrentHashMap<>();
    /** 手机号 → {code, expireAt} */
    private final Map<String, CodeEntry> codeMap = new ConcurrentHashMap<>();

    private static final long SEND_INTERVAL_MS = 60_000;  // 60s 频率限制
    private static final long CODE_TTL_MS = 300_000;       // 5 分钟有效期

    /** 是否可以发送（60s 内未发过） */
    public boolean canSend(String phone) {
        Long last = lastSendTime.get(phone);
        return last == null || System.currentTimeMillis() - last > SEND_INTERVAL_MS;
    }

    /** 返回剩余冷却秒数 */
    public long remainingCooldown(String phone) {
        Long last = lastSendTime.get(phone);
        if (last == null) return 0;
        long remain = SEND_INTERVAL_MS - (System.currentTimeMillis() - last);
        return Math.max(0, remain / 1000);
    }

    /** 存入验证码 */
    public void save(String phone, String code) {
        lastSendTime.put(phone, System.currentTimeMillis());
        codeMap.put(phone, new CodeEntry(code, System.currentTimeMillis() + CODE_TTL_MS));
    }

    /** 校验验证码，成功则删除防止复用 */
    public boolean verify(String phone, String code) {
        CodeEntry entry = codeMap.get(phone);
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expireAt) {
            codeMap.remove(phone);
            return false;
        }
        if (entry.code.equals(code)) {
            codeMap.remove(phone); // 一次性消费
            return true;
        }
        return false;
    }

    /** 生成6位数字验证码 */
    public String generateCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }

    private record CodeEntry(String code, long expireAt) {}
}
