package com.liuhecai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "liuhecai.draw")
public class DrawProperties {

    /**
     * 兼容旧配置：单一 URL（可含 {lotteryType} 占位）。
     * 优先使用 {@link #sources}；二者都空则公开源返回 empty。
     */
    private String httpUrl = "";

    /**
     * 按彩种配置公开源 URL 列表，按顺序尝试，首个解析成功即用。
     * key：MACAU_NEW / HK / MACAU_OLD
     */
    private Map<String, List<String>> sources = new LinkedHashMap<>();

    /** 原站往期开奖接口根地址（submit_ajax.ashx） */
    private String historyBaseUrl = "https://zhibo.77kj.vip";
}
