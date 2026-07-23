package com.liuhecai.job;

import com.liuhecai.service.DrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DrawFetchJob {

    private final DrawService drawService;

    /** 每小时整点拉取；失败只记日志，可手工触发/补录 */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledFetch() {
        try {
            Map<String, Object> summary = drawService.fetchAll();
            log.info("定时开奖拉取完成 {}", summary);
        } catch (Exception e) {
            log.error("定时开奖拉取失败", e);
        }
    }
}
