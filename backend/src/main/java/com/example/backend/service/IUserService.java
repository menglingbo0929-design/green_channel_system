package com.example.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.backend.model.domain.User;

/**
 * 用户服务接口
 *
 * 继承 MyBatis-Plus 的 IService<User>：
 * 自动获得 save、remove、update、getById、list 等常用 CRUD 方法，
 * 不需要在实现类里写这些基础操作。
 *
 * 这里只定义业务特有的方法（login），基础 CRUD 由 IService 提供。
 */
public interface IUserService extends IService<User> {

    /**
     * 用户登录认证
     *
     * 修改前：login(User user) — 传入整个 User 对象，用明文密码查库
     * 修改后：login(String loginName, String password) — 只传用户名和密码，
     *         密码用 BCrypt 加密后比对，不再明文查询
     *
     * @param loginName 用户名
     * @param password  明文密码（前端直接传来，后端负责加密校验）
     * @return 认证成功返回 User 实体，失败返回 null
     */
    User login(String loginName, String password);
}
