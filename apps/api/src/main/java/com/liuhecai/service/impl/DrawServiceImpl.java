package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.auth.AuthContext;
import com.liuhecai.auth.AuthUser;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.enums.LotteryType;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.util.ZodiacHelper;
import com.liuhecai.draw.DrawSource;
import com.liuhecai.draw.FetchedDraw;
import com.liuhecai.draw.HttpDrawSource;
import com.liuhecai.draw.MockDrawSource;
import com.liuhecai.draw.ZhiboHistoryClient;
import com.liuhecai.dto.DrawOverrideRequest;
import com.liuhecai.entity.DrawOverride;
import com.liuhecai.entity.DrawResultGlobal;
import com.liuhecai.mapper.DrawOverrideMapper;
import com.liuhecai.mapper.DrawResultGlobalMapper;
import com.liuhecai.service.DrawService;
import com.liuhecai.tenant.TenantContext;
import com.liuhecai.vo.DrawHistoryItemVO;
import com.liuhecai.vo.DrawResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawServiceImpl implements DrawService {

    private static final Map<String, String> LABELS = Map.of(
            LotteryType.MACAU_NEW.name(), "新澳门",
            LotteryType.HK.name(), "香港彩",
            LotteryType.MACAU_OLD.name(), "老澳门"
    );

    private final DrawResultGlobalMapper drawResultGlobalMapper;
    private final DrawOverrideMapper drawOverrideMapper;
    private final ObjectMapper objectMapper;
    private final HttpDrawSource httpDrawSource;
    private final MockDrawSource mockDrawSource;
    private final ZhiboHistoryClient zhiboHistoryClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> fetchAll() {
        Map<String, Object> summary = new LinkedHashMap<>();
        List<String> saved = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (LotteryType type : LotteryType.values()) {
            try {
                boolean ok = fetchOne(type.name());
                if (ok) {
                    saved.add(type.name());
                } else {
                    failed.add(type.name());
                }
            } catch (Exception e) {
                log.error("开奖拉取异常 lotteryType={}", type.name(), e);
                failed.add(type.name());
            }
        }
        summary.put("saved", saved);
        summary.put("failed", failed);
        return summary;
    }

    private boolean fetchOne(String lotteryType) {
        Optional<FetchedDraw> remote = safeFetch(httpDrawSource, lotteryType);
        FetchedDraw draw = remote.orElseGet(() -> {
            Optional<FetchedDraw> mock = safeFetch(mockDrawSource, lotteryType);
            if (mock.isEmpty()) {
                log.warn("mock 源也无数据 lotteryType={}", lotteryType);
                return null;
            }
            if (remote.isEmpty()) {
                log.info("公开源无数据，使用 mock lotteryType={}", lotteryType);
            }
            return mock.get();
        });
        if (draw == null) {
            return false;
        }
        upsertGlobal(draw);
        return true;
    }

    private Optional<FetchedDraw> safeFetch(DrawSource source, String lotteryType) {
        try {
            return source.fetchLatest(lotteryType);
        } catch (Exception e) {
            log.warn("源 {} 拉取失败 lotteryType={} msg={}", source.name(), lotteryType, e.getMessage());
            return Optional.empty();
        }
    }

    private void upsertGlobal(FetchedDraw draw) {
        DrawResultGlobal existing = drawResultGlobalMapper.selectOne(new LambdaQueryWrapper<DrawResultGlobal>()
                .eq(DrawResultGlobal::getLotteryType, draw.lotteryType())
                .eq(DrawResultGlobal::getIssueNo, draw.issueNo())
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        String numbersJson = toJson(padNumbers(draw.numbers()));
        String special = pad(draw.specialNumber());
        int yearly = draw.yearly() != null ? draw.yearly() : draw.drawTime().getYear();
        List<String> zodiacs = (draw.zodiacs() != null && draw.zodiacs().size() >= 7)
                ? draw.zodiacs()
                : ZodiacHelper.ofNumbers(draw.numbers(), draw.specialNumber(), yearly);
        String zodiacJson = toJson(zodiacs);
        if (existing == null) {
            DrawResultGlobal row = new DrawResultGlobal();
            row.setLotteryType(draw.lotteryType());
            row.setIssueNo(draw.issueNo());
            row.setDrawTime(draw.drawTime());
            row.setNumbersJson(numbersJson);
            row.setSpecialNumber(special);
            row.setZodiacJson(zodiacJson);
            row.setSource(draw.source());
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            drawResultGlobalMapper.insert(row);
        } else {
            existing.setDrawTime(draw.drawTime());
            existing.setNumbersJson(numbersJson);
            existing.setSpecialNumber(special);
            existing.setZodiacJson(zodiacJson);
            existing.setSource(draw.source());
            existing.setUpdatedAt(now);
            drawResultGlobalMapper.updateById(existing);
        }
        // 公开源成功后清掉同彩种 mock，避免假开奖时间盖住真实期号
        if ("http-public".equals(draw.source())) {
            drawResultGlobalMapper.delete(new LambdaQueryWrapper<DrawResultGlobal>()
                    .eq(DrawResultGlobal::getLotteryType, draw.lotteryType())
                    .eq(DrawResultGlobal::getSource, "mock"));
        }
    }

    @Override
    public DrawResultVO latest(String lotteryType) {
        String type = normalizeType(lotteryType);
        DrawOverride override = findOverride(type);
        if (override != null) {
            return toVo(override.getLotteryType(), override.getIssueNo(), override.getDrawTime(),
                    override.getNumbersJson(), override.getSpecialNumber(), override.getZodiacJson(),
                    "override", true);
        }
        DrawResultGlobal global = findLatestGlobal(type);
        if (global == null) {
            return emptyVo(type);
        }
        return toVo(global.getLotteryType(), global.getIssueNo(), global.getDrawTime(),
                global.getNumbersJson(), global.getSpecialNumber(), global.getZodiacJson(),
                global.getSource(), false);
    }

    /** 优先非 mock；同来源取开奖时间最新 */
    private DrawResultGlobal findLatestGlobal(String lotteryType) {
        DrawResultGlobal real = drawResultGlobalMapper.selectOne(new LambdaQueryWrapper<DrawResultGlobal>()
                .eq(DrawResultGlobal::getLotteryType, lotteryType)
                .ne(DrawResultGlobal::getSource, "mock")
                .orderByDesc(DrawResultGlobal::getDrawTime)
                .last("LIMIT 1"));
        if (real != null) {
            return real;
        }
        return drawResultGlobalMapper.selectOne(new LambdaQueryWrapper<DrawResultGlobal>()
                .eq(DrawResultGlobal::getLotteryType, lotteryType)
                .orderByDesc(DrawResultGlobal::getDrawTime)
                .last("LIMIT 1"));
    }

    @Override
    public List<DrawResultVO> latestAll() {
        return Arrays.stream(LotteryType.values())
                .map(t -> latest(t.name()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DrawHistoryItemVO> history(String lotteryType, Integer year, int pageSize) {
        String type = normalizeType(lotteryType);
        return zhiboHistoryClient.fetchHistory(type, year, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DrawResultVO override(DrawOverrideRequest request) {
        AuthUser user = AuthContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String type = normalizeType(request.getLotteryType());
        List<Integer> nums = request.getNumbers().stream()
                .map(this::parseBall)
                .collect(Collectors.toList());
        int special = parseBall(request.getSpecialNumber());
        validateBalls(nums, special);

        Long tenantId = user.getTenantId();
        TenantContext.set(tenantId);
        LocalDateTime now = LocalDateTime.now();
        String numbersJson = toJson(padNumbers(nums));
        String specialStr = pad(special);
        int yearly = request.getDrawTime() != null
                ? request.getDrawTime().getYear()
                : LocalDate.now().getYear();
        String zodiacJson = toJson(ZodiacHelper.ofNumbers(nums, special, yearly));

        DrawOverride existing = drawOverrideMapper.selectOne(new LambdaQueryWrapper<DrawOverride>()
                .eq(DrawOverride::getLotteryType, type)
                .eq(DrawOverride::getIssueNo, request.getIssueNo().trim())
                .last("LIMIT 1"));
        if (existing == null) {
            DrawOverride row = new DrawOverride();
            row.setTenantId(tenantId);
            row.setLotteryType(type);
            row.setIssueNo(request.getIssueNo().trim());
            row.setDrawTime(request.getDrawTime());
            row.setNumbersJson(numbersJson);
            row.setSpecialNumber(specialStr);
            row.setZodiacJson(zodiacJson);
            row.setNote(request.getNote());
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            drawOverrideMapper.insert(row);
        } else {
            existing.setDrawTime(request.getDrawTime());
            existing.setNumbersJson(numbersJson);
            existing.setSpecialNumber(specialStr);
            existing.setZodiacJson(zodiacJson);
            existing.setNote(request.getNote());
            existing.setUpdatedAt(now);
            drawOverrideMapper.updateById(existing);
        }
        return latest(type);
    }

    private DrawOverride findOverride(String lotteryType) {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            return null;
        }
        return drawOverrideMapper.selectOne(new LambdaQueryWrapper<DrawOverride>()
                .eq(DrawOverride::getLotteryType, lotteryType)
                .orderByDesc(DrawOverride::getDrawTime)
                .last("LIMIT 1"));
    }

    private DrawResultVO emptyVo(String type) {
        LocalDateTime next = nextDrawTime(null);
        return DrawResultVO.builder()
                .lotteryType(type)
                .lotteryLabel(LABELS.getOrDefault(type, type))
                .numbers(List.of())
                .zodiacs(List.of())
                .wuxings(List.of())
                .source(null)
                .overridden(false)
                .nextDrawTime(next)
                .countdownSeconds(Math.max(0, Duration.between(LocalDateTime.now(), next).getSeconds()))
                .build();
    }

    private DrawResultVO toVo(String type, String issueNo, LocalDateTime drawTime,
                              String numbersJson, String special, String zodiacJson,
                              String source, boolean overridden) {
        List<String> numberStrs = readStringList(numbersJson);
        List<Integer> numberInts = numberStrs.stream().map(this::parseBall).collect(Collectors.toList());
        int specialInt = StringUtils.hasText(special) ? parseBall(special) : 0;
        int yearly = resolveYearly(issueNo, drawTime);
        List<String> zodiacs = readStringList(zodiacJson);
        if (zodiacs.size() < 7 && !numberInts.isEmpty() && specialInt > 0) {
            zodiacs = ZodiacHelper.ofNumbers(numberInts, specialInt, yearly);
        }
        List<String> wuxings = (specialInt > 0 && numberInts.size() == 6)
                ? ZodiacHelper.wuxingsOf(numberInts, specialInt, yearly)
                : List.of();
        String displayIssue = toDisplayIssue(issueNo);
        String nextDisplay = nextDisplayIssue(displayIssue);
        String nextIssueNo = nextIssueNo(issueNo, nextDisplay);
        LocalDateTime next = nextDrawTime(drawTime);
        return DrawResultVO.builder()
                .lotteryType(type)
                .lotteryLabel(LABELS.getOrDefault(type, type))
                .issueNo(issueNo)
                .displayIssue(displayIssue)
                .drawTime(drawTime)
                .numbers(numberStrs)
                .specialNumber(special)
                .zodiacs(zodiacs)
                .wuxings(wuxings)
                .source(source)
                .overridden(overridden)
                .nextIssueNo(nextIssueNo)
                .nextDisplayIssue(nextDisplay)
                .nextDrawTime(next)
                .countdownSeconds(Math.max(0, Duration.between(LocalDateTime.now(), next).getSeconds()))
                .build();
    }

    private static int resolveYearly(String issueNo, LocalDateTime drawTime) {
        if (StringUtils.hasText(issueNo) && issueNo.length() >= 4
                && issueNo.substring(0, 4).chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(issueNo.substring(0, 4));
        }
        return drawTime != null ? drawTime.getYear() : LocalDate.now().getYear();
    }

    private static String toDisplayIssue(String issueNo) {
        if (!StringUtils.hasText(issueNo)) {
            return "";
        }
        String raw = issueNo.trim();
        if (raw.length() >= 7 && raw.chars().allMatch(Character::isDigit)) {
            return String.valueOf(Integer.parseInt(raw.substring(raw.length() - 3)));
        }
        if (raw.contains("-")) {
            String[] parts = raw.split("-");
            return parts[parts.length - 1].replaceFirst("^0+(?!$)", "");
        }
        return raw.replaceFirst("^0+(?!$)", "");
    }

    private static String nextDisplayIssue(String displayIssue) {
        if (!StringUtils.hasText(displayIssue)) {
            return "";
        }
        try {
            return String.valueOf(Integer.parseInt(displayIssue) + 1);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private static String nextIssueNo(String issueNo, String nextDisplay) {
        if (!StringUtils.hasText(nextDisplay)) {
            return null;
        }
        if (StringUtils.hasText(issueNo) && issueNo.length() >= 7
                && issueNo.chars().allMatch(Character::isDigit)) {
            String year = issueNo.substring(0, 4);
            try {
                return year + String.format("%03d", Integer.parseInt(nextDisplay));
            } catch (NumberFormatException ignored) {
                return nextDisplay;
            }
        }
        return nextDisplay;
    }

    private LocalDateTime nextDrawTime(LocalDateTime lastDraw) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayDraw = LocalDateTime.of(LocalDate.now(), LocalTime.of(21, 30));
        if (now.isBefore(todayDraw)) {
            return todayDraw;
        }
        if (lastDraw != null && lastDraw.toLocalDate().equals(LocalDate.now())
                && lastDraw.toLocalTime().isAfter(LocalTime.of(20, 0))) {
            return todayDraw.plusDays(1);
        }
        return todayDraw.plusDays(1);
    }

    private String normalizeType(String lotteryType) {
        if (!StringUtils.hasText(lotteryType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "lotteryType 不能为空");
        }
        String type = lotteryType.trim().toUpperCase();
        try {
            LotteryType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的彩种: " + lotteryType);
        }
        return type;
    }

    private int parseBall(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "号码格式错误: " + raw);
        }
    }

    private void validateBalls(List<Integer> numbers, int special) {
        if (numbers.size() != 6) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "正码必须 6 个");
        }
        for (Integer n : numbers) {
            if (n < 1 || n > 49) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "号码范围 1-49");
            }
        }
        if (special < 1 || special > 49) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "特码范围 1-49");
        }
        if (numbers.stream().distinct().count() != 6) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "正码不能重复");
        }
        if (numbers.contains(special)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "特码不能与正码重复");
        }
    }

    private List<String> padNumbers(List<Integer> numbers) {
        return numbers.stream().map(this::pad).collect(Collectors.toList());
    }

    private String pad(int n) {
        return String.format("%02d", n);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "JSON 序列化失败");
        }
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
