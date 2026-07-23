package com.liuhecai.draw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.enums.LotteryType;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.util.ZodiacHelper;
import com.liuhecai.config.DrawProperties;
import com.liuhecai.vo.DrawHistoryItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 代理原站往期开奖：{@code /tools/submit_ajax.ashx?action=getkjrecords}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZhiboHistoryClient {

    private static final Map<String, String> CODE_MAP = Map.of(
            LotteryType.MACAU_NEW.name(), "am",
            LotteryType.HK.name(), "xg",
            LotteryType.MACAU_OLD.name(), "48am"
    );

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final DrawProperties drawProperties;

    public List<DrawHistoryItemVO> fetchHistory(String lotteryType, Integer year, int pageSize) {
        String code = CODE_MAP.get(lotteryType);
        if (code == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的彩种: " + lotteryType);
        }
        String base = drawProperties.getHistoryBaseUrl();
        if (!StringUtils.hasText(base)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未配置往期开奖地址");
        }
        StringBuilder url = new StringBuilder(base.replaceAll("/$", ""));
        url.append("/tools/submit_ajax.ashx?action=getkjrecords&code=").append(code);
        url.append("&pagesize=").append(Math.min(Math.max(pageSize, 1), 500));
        if (year != null && year > 2000) {
            url.append("&year=").append(year);
        }
        url.append("&rand=").append(System.currentTimeMillis());
        try {
            String body = restClientBuilder.build().get()
                    .uri(URI.create(url.toString()))
                    .header("User-Agent", "Mozilla/5.0 (compatible; LiuhecaiDrawBot/1.0)")
                    .header("Accept", "application/json,text/plain,*/*")
                    .retrieve()
                    .body(String.class);
            return parse(body);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("往期开奖拉取失败 type={} msg={}", lotteryType, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "往期开奖拉取失败");
        }
    }

    private List<DrawHistoryItemVO> parse(String body) throws Exception {
        if (!StringUtils.hasText(body)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "往期开奖无数据");
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.get("data");
        if (data == null || !data.isArray()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "往期开奖数据格式错误");
        }
        List<DrawHistoryItemVO> list = new ArrayList<>();
        for (JsonNode row : data) {
            String qishu = text(row, "qishu", "periods");
            String num = text(row, "num", "openCode");
            if (!StringUtils.hasText(qishu) || !StringUtils.hasText(num)) {
                continue;
            }
            Integer yearly = parseInt(text(row, "yearly", "year"));
            if (yearly == null) {
                yearly = java.time.LocalDate.now().getYear();
            }
            String[] parts = num.split("[,，\\s]+");
            if (parts.length < 7) {
                continue;
            }
            List<String> numbers = new ArrayList<>();
            List<Integer> ints = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                int n = Integer.parseInt(parts[i].trim());
                ints.add(n);
                numbers.add(String.format(Locale.ROOT, "%02d", n));
            }
            int specialInt = Integer.parseInt(parts[6].trim());
            String special = String.format(Locale.ROOT, "%02d", specialInt);
            List<String> zodiacs = splitOrCompute(text(row, "shengxiao", "zodiac"), ints, specialInt, yearly, true);
            List<String> wuxings = splitOrCompute(text(row, "wuxing"), ints, specialInt, yearly, false);
            String issueNo = yearly + String.format(Locale.ROOT, "%03d", Integer.parseInt(qishu.trim()));
            list.add(DrawHistoryItemVO.builder()
                    .issueNo(issueNo)
                    .displayIssue(stripLeadingZeros(qishu.trim()))
                    .drawDate(text(row, "date", "openTime", "times"))
                    .numbers(numbers)
                    .specialNumber(special)
                    .zodiacs(zodiacs)
                    .wuxings(wuxings)
                    .build());
        }
        return list;
    }

    private static List<String> splitOrCompute(String csv, List<Integer> nums, int special, int yearly, boolean zodiac) {
        if (StringUtils.hasText(csv)) {
            String[] parts = csv.split("[,，]");
            List<String> out = new ArrayList<>();
            for (String p : parts) {
                if (StringUtils.hasText(p)) {
                    out.add(p.trim());
                }
            }
            if (out.size() >= 7) {
                return out.subList(0, 7);
            }
        }
        return zodiac
                ? ZodiacHelper.ofNumbers(nums, special, yearly)
                : ZodiacHelper.wuxingsOf(nums, special, yearly);
    }

    private static String stripLeadingZeros(String raw) {
        try {
            return String.valueOf(Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private static Integer parseInt(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
}
