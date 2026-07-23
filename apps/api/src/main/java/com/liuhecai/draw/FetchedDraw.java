package com.liuhecai.draw;

import java.time.LocalDateTime;
import java.util.List;

public record FetchedDraw(
        String lotteryType,
        String issueNo,
        LocalDateTime drawTime,
        List<Integer> numbers,
        int specialNumber,
        String source,
        Integer yearly,
        String nextIssueNo,
        LocalDateTime nextDrawTime,
        List<String> zodiacs,
        List<String> wuxings
) {
    public FetchedDraw(
            String lotteryType,
            String issueNo,
            LocalDateTime drawTime,
            List<Integer> numbers,
            int specialNumber,
            String source
    ) {
        this(lotteryType, issueNo, drawTime, numbers, specialNumber, source,
                null, null, null, null, null);
    }
}
