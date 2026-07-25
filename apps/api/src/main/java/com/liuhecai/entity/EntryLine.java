package com.liuhecai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("entry_lines")
public class EntryLine {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** domains.id，且 role=ENTRY */
    private Long entryDomainId;
    private Integer sortOrder;
    private String label;
    private String color;
    private Long targetTenantId;
    /** 1 启用 0 停用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
