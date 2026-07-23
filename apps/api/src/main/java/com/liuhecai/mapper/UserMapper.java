package com.liuhecai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuhecai.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
