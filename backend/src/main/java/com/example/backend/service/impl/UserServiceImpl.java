package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.domain.User;
import com.example.backend.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements IUserService {
@Autowired
UserMapper userMapper;
    public User login(User user) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        queryWrapper.eq(User::getLoginName,user.getLoginName())
                .eq(User::getPassword,user.getPassword());
        User one =userMapper.selectOne(queryWrapper);
        return one;
    }


}
