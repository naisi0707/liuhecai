package com.liuhecai.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TopicVO {
    private String id;
    private String title;
    private String lotteryType;
    private String issueNo;
    private String playType;
    private Integer price;
    private Integer status;
    private boolean purchased;
    private boolean contentVisible;
    /** 往期成绩等公开预览，未购买也返回 */
    private String previewContent;
    private String content;
    private Integer viewCount;
    private Integer purchaseCount;
    private String prevTopicId;
    private String prevTopicTitle;
    private String nextTopicId;
    private String nextTopicTitle;
    private LocalDateTime createdAt;
}
