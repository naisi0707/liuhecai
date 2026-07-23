package com.liuhecai.draw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.common.util.ZodiacHelper;
import com.liuhecai.config.DrawProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 拉取公开开奖接口。优先解析原站 zhibo {@code /kj/data/*.js}，
 * 兼容 macaumarksix 数组与 1234kj {@code {code,data}}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpDrawSource implements DrawSource {

    private static final DateTimeFormatter[] DRAW_TIME_FMTS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd H:mm:ss")
    };
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; LiuhecaiDrawBot/1.0)";

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final DrawProperties drawProperties;

    @Override
    public String name() {
        return "http-public";
    }

    @Override
    public Optional<FetchedDraw> fetchLatest(String lotteryType) {
        List<String> urls = resolveUrls(lotteryType);
        if (urls.isEmpty()) {
            return Optional.empty();
        }
        RestClient client = restClientBuilder.build();
        for (String url : urls) {
            try {
                String body = client.get()
                        .uri(URI.create(url))
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json,text/plain,*/*")
                        .retrieve()
                        .body(String.class);
                if (!StringUtils.hasText(body)) {
                    log.warn("公开源空响应 lotteryType={} url={}", lotteryType, url);
                    continue;
                }
                Optional<FetchedDraw> parsed = parseBody(lotteryType, body);
                if (parsed.isPresent()) {
                    log.info("公开源成功 lotteryType={} issueNo={} url={}",
                            lotteryType, parsed.get().issueNo(), url);
                    return parsed;
                }
                log.warn("公开源无法解析 lotteryType={} url={} bodyLen={}",
                        lotteryType, url, body.length());
            } catch (Exception e) {
                log.warn("公开源拉取失败 lotteryType={} url={} msg={}",
                        lotteryType, url, e.getMessage());
            }
        }
        return Optional.empty();
    }

    private List<String> resolveUrls(String lotteryType) {
        List<String> fromMap = drawProperties.getSources() == null
                ? List.of()
                : drawProperties.getSources().getOrDefault(lotteryType, List.of());
        List<String> urls = new ArrayList<>();
        for (String u : fromMap) {
            if (StringUtils.hasText(u)) {
                String trimmed = u.trim();
                if (trimmed.contains("?")) {
                    urls.add(trimmed + "&_=" + System.currentTimeMillis());
                } else if (trimmed.endsWith(".js")) {
                    urls.add(trimmed + "?rand=" + System.currentTimeMillis());
                } else {
                    urls.add(trimmed);
                }
            }
        }
        String legacy = drawProperties.getHttpUrl();
        if (StringUtils.hasText(legacy)) {
            urls.add(legacy.trim().replace("{lotteryType}", lotteryType));
        }
        return urls;
    }

    private Optional<FetchedDraw> parseBody(String lotteryType, String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (root.isArray() && !root.isEmpty()) {
            return parseGenericRecord(lotteryType, root.get(0));
        }
        if (root.isObject()) {
            // 原站 zhibo: { code, periods, num, yearly, nexttime, ... }
            if (root.has("periods") && root.has("num")) {
                return parseZhibo(lotteryType, root);
            }
            JsonNode data = root.get("data");
            if (data != null && data.isArray() && !data.isEmpty()) {
                return parseGenericRecord(lotteryType, data.get(0));
            }
            if (data != null && data.isObject()) {
                if (data.has("periods") && data.has("num")) {
                    return parseZhibo(lotteryType, data);
                }
                return parseGenericRecord(lotteryType, data);
            }
            if (root.has("openCode") || root.has("opencode")) {
                return parseGenericRecord(lotteryType, root);
            }
        }
        return Optional.empty();
    }

    private Optional<FetchedDraw> parseZhibo(String lotteryType, JsonNode node) {
        String periods = text(node, "periods", "qishu");
        String openCode = text(node, "num", "openCode", "opencode");
        if (!StringUtils.hasText(periods) || !StringUtils.hasText(openCode)) {
            return Optional.empty();
        }
        Integer yearly = intOrNull(node, "yearly", "year");
        if (yearly == null) {
            yearly = LocalDateTime.now().getYear();
        }
        String issueNo = yearly + String.format(Locale.ROOT, "%03d", Integer.parseInt(periods.trim()));
        List<Integer> all = parseNumbers(openCode);
        if (all.size() < 7) {
            return Optional.empty();
        }
        List<Integer> numbers = new ArrayList<>(all.subList(0, 6));
        int special = all.get(6);
        LocalDateTime drawTime = parseDrawTime(text(node, "times", "openTime", "time"));
        String nextIssue = text(node, "nextperiods", "nextqi");
        if (StringUtils.hasText(nextIssue)) {
            try {
                nextIssue = yearly + String.format(Locale.ROOT, "%03d", Integer.parseInt(nextIssue.trim()));
            } catch (NumberFormatException ignored) {
                // keep raw
            }
        }
        LocalDateTime nextDraw = parseDrawTime(text(node, "nexttime"));
        List<String> zodiacs = ZodiacHelper.ofNumbers(numbers, special, yearly);
        List<String> wuxings = ZodiacHelper.wuxingsOf(numbers, special, yearly);
        return Optional.of(new FetchedDraw(
                lotteryType, issueNo, drawTime, numbers, special, name(),
                yearly, nextIssue, nextDraw, zodiacs, wuxings
        ));
    }

    private Optional<FetchedDraw> parseGenericRecord(String lotteryType, JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        String issueNo = text(node, "expect", "issue", "issueNo", "qihao", "periods");
        String openCode = text(node, "openCode", "opencode", "code", "numbers", "num");
        String openTime = text(node, "openTime", "opentime", "drawTime", "time", "times");
        if (!StringUtils.hasText(issueNo) || !StringUtils.hasText(openCode)) {
            return Optional.empty();
        }
        List<Integer> all = parseNumbers(openCode);
        if (all.size() < 7) {
            return Optional.empty();
        }
        List<Integer> numbers = new ArrayList<>(all.subList(0, 6));
        int special = all.get(6);
        LocalDateTime drawTime = parseDrawTime(openTime);
        Integer yearly = intOrNull(node, "yearly", "year");
        if (yearly == null && issueNo.length() >= 4 && issueNo.chars().allMatch(Character::isDigit)) {
            yearly = Integer.parseInt(issueNo.substring(0, 4));
        }
        if (yearly == null) {
            yearly = drawTime.getYear();
        }
        List<String> apiZodiac = splitCsv(text(node, "zodiac", "shengxiao"));
        List<String> zodiacs = (apiZodiac.size() >= 7)
                ? apiZodiac.subList(0, 7)
                : ZodiacHelper.ofNumbers(numbers, special, yearly);
        List<String> wuxings = ZodiacHelper.wuxingsOf(numbers, special, yearly);
        return Optional.of(new FetchedDraw(
                lotteryType, issueNo.trim(), drawTime, numbers, special, name(),
                yearly, null, null, zodiacs, wuxings
        ));
    }

    private static Integer intOrNull(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode v = node.get(field);
            if (v != null && !v.isNull() && StringUtils.hasText(v.asText())) {
                try {
                    return Integer.parseInt(v.asText().trim());
                } catch (NumberFormatException ignored) {
                    // try next
                }
            }
        }
        return null;
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode v = node.get(field);
            if (v != null && !v.isNull() && StringUtils.hasText(v.asText())) {
                return v.asText().trim();
            }
        }
        return null;
    }

    private static List<String> splitCsv(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String[] parts = raw.split("[,，]");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (StringUtils.hasText(p)) {
                out.add(p.trim());
            }
        }
        return out;
    }

    private static List<Integer> parseNumbers(String openCode) {
        String[] parts = openCode.split("[,，\\s]+");
        List<Integer> nums = new ArrayList<>();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            nums.add(Integer.parseInt(part.trim()));
        }
        return nums;
    }

    private static LocalDateTime parseDrawTime(String openTime) {
        if (!StringUtils.hasText(openTime)) {
            return LocalDateTime.now();
        }
        String normalized = openTime.trim().replace('T', ' ');
        if (normalized.length() > 19) {
            normalized = normalized.substring(0, 19);
        }
        for (DateTimeFormatter fmt : DRAW_TIME_FMTS) {
            try {
                return LocalDateTime.parse(normalized, fmt);
            } catch (Exception ignored) {
                // try next
            }
        }
        return LocalDateTime.now();
    }
}
