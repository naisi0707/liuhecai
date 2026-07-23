package com.liuhecai.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuhecai.entity.OpAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface OpAuditLogMapper extends BaseMapper<OpAuditLog> {
}
