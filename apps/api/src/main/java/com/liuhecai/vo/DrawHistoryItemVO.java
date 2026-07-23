package com.liuhecai.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DrawHistoryItemVO {
    private String issueNo;
    private String displayIssue;
    private String drawDate;
    private List<String> numbers;
    private String specialNumber;
    private List<String> zodiacs;
    private List<String> wuxings;
}
