package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.config.VerificationCodeStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/verification-code")
@RequiredArgsConstructor
public class VerificationCodeController {

    private final VerificationCodeStore store;

    /** 发送验证码 */
    @PostMapping("send")
    public JsonResponse<Void> send(@RequestParam("phone") String phone) {
        if (phone == null || phone.length() < 11) {
            return JsonResponse.failure("请输入正确的手机号");
        }
        if (!store.canSend(phone)) {
            long sec = store.remainingCooldown(phone);
            return JsonResponse.failure(sec + "秒后可重新发送");
        }
        String code = store.generateCode();
        store.save(phone, code);

        // 开发环境：打印到控制台 + 日志
        log.info("===== 验证码 [{}] => {} =====", phone, code);
        System.out.println("验证码 [ " + phone + " ] => " + code + " (5分钟内有效)");

        return JsonResponse.successMessage("验证码已发送");
    }
}
