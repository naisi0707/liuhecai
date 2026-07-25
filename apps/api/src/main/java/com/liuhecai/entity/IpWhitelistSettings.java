package com.liuhecai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ip_whitelist_settings")
public class IpWhitelistSettings {
    @TableId(type = IdType.INPUT)
    private Long id;
    /** 0 关闭（全放行） 1 启用 */
    private Integer enabled;
    private LocalDateTime updatedAt;
}
