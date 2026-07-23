package com.liuhecai.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseResultVO {
    private String topicId;
    private Integer price;
    private Integer coinBalance;
    private boolean alreadyPurchased;
}
