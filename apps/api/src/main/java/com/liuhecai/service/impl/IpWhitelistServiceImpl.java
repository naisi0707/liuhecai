package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.util.ClientIpResolver;
import com.liuhecai.common.util.Ipv4CidrMatcher;
import com.liuhecai.dto.IpWhitelistEntryRequest;
import com.liuhecai.dto.IpWhitelistUpdateRequest;
import com.liuhecai.entity.IpWhitelistEntry;
import com.liuhecai.entity.IpWhitelistSettings;
import com.liuhecai.mapper.IpWhitelistEntryMapper;
import com.liuhecai.mapper.IpWhitelistSettingsMapper;
import com.liuhecai.service.IpWhitelistService;
import com.liuhecai.vo.IpWhitelistEntryVO;
import com.liuhecai.vo.IpWhitelistVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class IpWhitelistServiceImpl implements IpWhitelistService {

    private static final long SETTINGS_ID = 1L;
    private static final long CACHE_TTL_MS = 5_000L;

    private final IpWhitelistSettingsMapper settingsMapper;
    private final IpWhitelistEntryMapper entryMapper;

    private final AtomicReference<CachedSnapshot> cache = new AtomicReference<>();

    @Override
    public boolean isAllowed(HttpServletRequest request) {
        Snapshot snap = loadSnapshot();
        if (!snap.enabled) {
            return true;
        }
        String ip = ClientIpResolver.resolve(request);
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        for (String cidr : snap.cidrs) {
            if (Ipv4CidrMatcher.matches(ip, cidr)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IpWhitelistVO getConfig(HttpServletRequest request) {
        ensureSettingsRow();
        IpWhitelistSettings settings = settingsMapper.selectById(SETTINGS_ID);
        List<IpWhitelistEntry> entries = entryMapper.selectList(new LambdaQueryWrapper<IpWhitelistEntry>()
                .orderByAsc(IpWhitelistEntry::getId));
        return toVo(settings != null && settings.getEnabled() != null && settings.getEnabled() == 1,
                ClientIpResolver.resolve(request),
                entries);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IpWhitelistVO replace(IpWhitelistUpdateRequest request, HttpServletRequest httpRequest) {
        ensureSettingsRow();
        String currentIp = ClientIpResolver.resolve(httpRequest);
        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        List<IpWhitelistEntryRequest> raw = request.getEntries() == null ? List.of() : request.getEntries();
        List<NormalizedEntry> normalized = new ArrayList<>();
        for (IpWhitelistEntryRequest item : raw) {
            if (item == null || !StringUtils.hasText(item.getCidr())) {
                continue;
            }
            String cidr = item.getCidr().trim();
            if (!Ipv4CidrMatcher.isValidCidrOrIp(cidr)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "无效 IP/CIDR: " + cidr);
            }
            String note = StringUtils.hasText(item.getNote()) ? item.getNote().trim() : null;
            normalized.add(new NormalizedEntry(cidr, note));
        }
        if (enabled) {
            if (normalized.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "启用白名单前请至少添加一条 IP");
            }
            boolean coversCurrent = false;
            if (StringUtils.hasText(currentIp)) {
                for (NormalizedEntry e : normalized) {
                    if (Ipv4CidrMatcher.matches(currentIp, e.cidr)) {
                        coversCurrent = true;
                        break;
                    }
                }
            }
            if (!coversCurrent) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "启用前请先将当前 IP（" + (currentIp == null ? "未知" : currentIp) + "）加入白名单，以免锁死");
            }
        }

        entryMapper.delete(new LambdaQueryWrapper<>());
        LocalDateTime now = LocalDateTime.now();
        for (NormalizedEntry e : normalized) {
            IpWhitelistEntry row = new IpWhitelistEntry();
            row.setCidr(e.cidr);
            row.setNote(e.note);
            row.setCreatedAt(now);
            entryMapper.insert(row);
        }

        IpWhitelistSettings settings = settingsMapper.selectById(SETTINGS_ID);
        if (settings == null) {
            settings = new IpWhitelistSettings();
            settings.setId(SETTINGS_ID);
            settings.setEnabled(enabled ? 1 : 0);
            settings.setUpdatedAt(now);
            settingsMapper.insert(settings);
        } else {
            settings.setEnabled(enabled ? 1 : 0);
            settings.setUpdatedAt(now);
            settingsMapper.updateById(settings);
        }
        invalidateCache();
        return getConfig(httpRequest);
    }

    @Override
    public void invalidateCache() {
        cache.set(null);
    }

    private Snapshot loadSnapshot() {
        long now = System.currentTimeMillis();
        CachedSnapshot cached = cache.get();
        if (cached != null && now - cached.loadedAtMs < CACHE_TTL_MS) {
            return cached.snapshot;
        }
        ensureSettingsRow();
        IpWhitelistSettings settings = settingsMapper.selectById(SETTINGS_ID);
        boolean enabled = settings != null && settings.getEnabled() != null && settings.getEnabled() == 1;
        List<String> cidrs = entryMapper.selectList(new LambdaQueryWrapper<IpWhitelistEntry>()
                        .orderByAsc(IpWhitelistEntry::getId))
                .stream()
                .map(IpWhitelistEntry::getCidr)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        Snapshot snap = new Snapshot(enabled, cidrs);
        cache.set(new CachedSnapshot(now, snap));
        return snap;
    }

    private void ensureSettingsRow() {
        IpWhitelistSettings existing = settingsMapper.selectById(SETTINGS_ID);
        if (existing != null) {
            return;
        }
        IpWhitelistSettings row = new IpWhitelistSettings();
        row.setId(SETTINGS_ID);
        row.setEnabled(0);
        row.setUpdatedAt(LocalDateTime.now());
        try {
            settingsMapper.insert(row);
        } catch (Exception ignored) {
            // concurrent create
        }
    }

    private static IpWhitelistVO toVo(boolean enabled, String currentIp, List<IpWhitelistEntry> entries) {
        IpWhitelistVO vo = new IpWhitelistVO();
        vo.setEnabled(enabled);
        vo.setCurrentIp(currentIp);
        List<IpWhitelistEntryVO> list = new ArrayList<>();
        for (IpWhitelistEntry e : entries) {
            IpWhitelistEntryVO item = new IpWhitelistEntryVO();
            item.setId(e.getId() == null ? null : String.valueOf(e.getId()));
            item.setCidr(e.getCidr());
            item.setNote(e.getNote());
            list.add(item);
        }
        vo.setEntries(list);
        return vo;
    }

    private record NormalizedEntry(String cidr, String note) {
    }

    private record Snapshot(boolean enabled, List<String> cidrs) {
    }

    private record CachedSnapshot(long loadedAtMs, Snapshot snapshot) {
    }
}
