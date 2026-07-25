package com.liuhecai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuhecai.entity.IpWhitelistEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IpWhitelistEntryMapper extends BaseMapper<IpWhitelistEntry> {
}
