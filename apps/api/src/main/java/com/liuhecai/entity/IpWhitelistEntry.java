package com.liuhecai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ip_whitelist_entries")
public class IpWhitelistEntry {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 单 IP 或 CIDR，如 1.2.3.4 或 1.2.3.0/24 */
    private String cidr;
    private String note;
    private LocalDateTime createdAt;
}
