package com.liuhecai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserOrderVO {
    private Long id;
    private Long topicId;
    private String topicTitle;
    private Integer price;
    private LocalDateTime createdAt;
}
