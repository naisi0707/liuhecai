package com.liuhecai.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface TokenVersionMapper {

    @Update("UPDATE super_admins SET token_version = token_version + 1 WHERE id = #{id}")
    int bumpSuperTokenVersion(@Param("id") Long id);

    @Select("SELECT token_version FROM super_admins WHERE id = #{id}")
    Integer selectSuperTokenVersion(@Param("id") Long id);
}
