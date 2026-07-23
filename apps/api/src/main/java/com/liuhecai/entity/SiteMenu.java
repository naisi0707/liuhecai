package com.liuhecai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_menus")
public class SiteMenu {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String code;
    private String title;
    private String path;
    private Integer sortNo;
    /** 1 显示 0 隐藏 */
    private Integer visible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
