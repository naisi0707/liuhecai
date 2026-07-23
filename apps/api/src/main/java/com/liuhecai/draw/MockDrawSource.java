package com.liuhecai.draw;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 演示用 mock 源：按彩种+日期生成稳定号码，保证 Job 可写入至少一期。
 */
@Component
public class MockDrawSource implements DrawSource {

    private static final DateTimeFormatter ISSUE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public Optional<FetchedDraw> fetchLatest(String lotteryType) {
        LocalDate today = LocalDate.now();
        LocalDateTime drawTime = LocalDateTime.of(today, LocalTime.of(21, 30));
        if (LocalDateTime.now().isBefore(drawTime)) {
            drawTime = LocalDateTime.of(today.minusDays(1), LocalTime.of(21, 30));
        }
        String issueNo = drawTime.toLocalDate().format(ISSUE_FMT);
        Random random = new Random((lotteryType + issueNo).hashCode());
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= 49; i++) {
            pool.add(i);
        }
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            numbers.add(pool.remove(random.nextInt(pool.size())));
        }
        numbers.sort(Integer::compareTo);
        int special = pool.get(random.nextInt(pool.size()));
        return Optional.of(new FetchedDraw(lotteryType, issueNo, drawTime, numbers, special, name()));
    }
}
