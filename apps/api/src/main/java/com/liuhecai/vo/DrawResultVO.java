package com.liuhecai.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DrawResultVO {
    private String lotteryType;
    private String lotteryLabel;
    private String issueNo;
    /** 展示用短期号，如 203 / 078 */
    private String displayIssue;
    private LocalDateTime drawTime;
    private List<String> numbers;
    private String specialNumber;
    private List<String> zodiacs;
    private List<String> wuxings;
    private String source;
    private boolean overridden;
    private String nextIssueNo;
    private String nextDisplayIssue;
    private LocalDateTime nextDrawTime;
    private long countdownSeconds;
}
