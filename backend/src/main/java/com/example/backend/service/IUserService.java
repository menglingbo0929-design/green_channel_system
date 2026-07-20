package com.example.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.backend.model.domain.User;
import com.example.backend.model.dto.UserVO;

import java.util.List;

public interface IUserService extends IService<User> {

    /** 登录认证 */
    User login(String loginName, String password);

    /** 查询所有用户（含角色） */
    List<UserVO> listUsers();

    /** 创建用户 + 分配角色 */
    void createUser(String loginName, String password, String remark, List<Long> roleIds);

    /** 更新用户信息 + 角色 */
    void updateUser(Long userId, String loginName, String remark, List<Long> roleIds);

    /** 切换启用/停用 */
    void toggleStatus(Long userId);
}
