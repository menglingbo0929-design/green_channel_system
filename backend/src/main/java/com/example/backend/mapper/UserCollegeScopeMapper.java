package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.domain.UserCollegeScope;
import org.apache.ibatis.annotations.Select;

public interface UserCollegeScopeMapper extends BaseMapper<UserCollegeScope> {
    @Select("SELECT college_id FROM user_college_scope WHERE user_id=#{userId} ORDER BY id LIMIT 1")
    Long findCollegeIdByUserId(Long userId);
}
