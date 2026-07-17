package com.example.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.backend.model.domain.User;

public interface IUserService extends IService <User>
{
    User login(User user);
}
