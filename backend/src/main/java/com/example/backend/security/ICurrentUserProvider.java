package com.example.backend.security;

import com.example.backend.model.dto.LoginUser;

/**
 * 当前用户提供者接口 —— 成员一对外暴露的核心契约
 *
 * 其他模块注入此接口获取当前登录用户，
 * 不依赖具体实现，方便单元测试 mock。
 */
public interface ICurrentUserProvider {
    LoginUser getRequiredUser();
}
