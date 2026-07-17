package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.domain.User;
import com.example.backend.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/user")

public class UserController {
@Autowired
private IUserService userService;
    @PostMapping("login")
    public JsonResponse login(@RequestBody User user){
        User login=userService.login(user);
        if (login == null) {
            return JsonResponse.failure("用户名或密码错误");
        }
        return JsonResponse.success(login);
    }

}




