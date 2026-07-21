package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.backend.mapper.UserMapper;
import com.example.backend.mapper.UserRoleMapper;
import com.example.backend.model.domain.User;
import com.example.backend.model.domain.UserRole;
import com.example.backend.model.dto.UserVO;
import com.example.backend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User login(String loginName, String password) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLoginName, loginName)
                     .eq(User::getDeleted, 0L);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        return user;
    }

    @Override
    public List<UserVO> listUsers() {
        List<User> users = userMapper.selectList(null);
        return users.stream().map(user -> {
            List<String> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());
            List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, user.getId())
            ).stream().map(UserRole::getRoleId).collect(Collectors.toList());
            return new UserVO(
                user.getId(), user.getLoginName(), user.getRemark(),
                roles, roleIds, user.getDeleted(),
                user.getLastLoginTime(), user.getGmtCreated()
            );
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createUser(String loginName, String password, String remark, List<Long> roleIds) {
        User user = new User();
        user.setLoginName(loginName);
        user.setPassword(passwordEncoder.encode(password));
        user.setRemark(remark);
        user.setGmtCreated(LocalDateTime.now());
        user.setGmtModified(LocalDateTime.now());
        userMapper.insert(user);

        // 分配角色
        for (Long roleId : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(user.getId());
            ur.setRoleId(roleId);
            ur.setCreateTime(LocalDateTime.now());
            userRoleMapper.insert(ur);
        }
    }

    @Override
    @Transactional
    public void updateUser(Long userId, String loginName, String remark, List<Long> roleIds) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        if (loginName != null) user.setLoginName(loginName);
        if (remark != null) user.setRemark(remark);
        user.setGmtModified(LocalDateTime.now());
        userMapper.updateById(user);

        // 先删旧角色，再插新角色
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            ur.setCreateTime(LocalDateTime.now());
            userRoleMapper.insert(ur);
        }
    }

    @Override
    public void toggleStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        // 0→1 停用, 非0→0 启用
        user.setDeleted(user.getDeleted() == 0 ? 1L : 0L);
        userMapper.updateById(user);
    }

    @Override
    public boolean changePassword(Long userId, String oldPwd, String newPwd) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) return false;
        user.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
        return true;
    }
}
