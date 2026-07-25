package com.liuhecai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("topics")
public class Topic {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String title;
    private String lotteryType;
    private String issueNo;
    private String playType;
    /** 帖子标签：出售帖/高手帖/普通帖/推荐帖等 */
    private String tag;
    private Integer price;
    private String content;
    /** 往期成绩等公开预览（未购买也可见） */
    private String previewContent;
    /** 浏览量 */
    private Integer viewCount;
    /** 0待审 1已通过 2拒绝 3下架 */
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
