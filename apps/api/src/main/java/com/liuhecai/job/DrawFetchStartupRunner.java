package com.liuhecai.job;

import com.liuhecai.service.DrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 可选：启动时拉取一次开奖（liuhecai.draw.fetch-on-startup=true）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DrawFetchStartupRunner implements ApplicationRunner {

    private final DrawService drawService;

    @Value("${liuhecai.draw.fetch-on-startup:false}")
    private boolean fetchOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!fetchOnStartup) {
            return;
        }
        try {
            Map<String, Object> summary = drawService.fetchAll();
            log.info("启动开奖拉取完成 {}", summary);
        } catch (Exception e) {
            log.error("启动开奖拉取失败", e);
        }
    }
}
