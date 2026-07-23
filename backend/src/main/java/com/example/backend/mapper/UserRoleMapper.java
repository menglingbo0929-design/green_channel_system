package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.domain.UserRoleRelation;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserRoleMapper extends BaseMapper<UserRoleRelation> {

    /**
     * 查某个用户的所有角色编码
     *
     * 联表查询：sys_user_role JOIN sys_role
     * 返回 ["STUDENT", "COUNSELOR"] 这样的列表
     *
     * @param userId 用户 ID
     * @return 角色编码列表
     */
    @Select("SELECT r.role_code FROM sys_user_role ur " +
            "JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(Long userId);
}
