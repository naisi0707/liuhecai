package com.liuhecai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("draw_results_global")
public class DrawResultGlobal {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String lotteryType;
    private String issueNo;
    private LocalDateTime drawTime;
    private String numbersJson;
    private String specialNumber;
    private String zodiacJson;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
