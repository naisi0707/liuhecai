package com.liuhecai.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.config.CacheConfig;
import com.liuhecai.entity.Domain;
import com.liuhecai.entity.Tenant;
import com.liuhecai.mapper.DomainMapper;
import com.liuhecai.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DomainTenantLookup {

    private final DomainMapper domainMapper;
    private final TenantMapper tenantMapper;

    @Cacheable(cacheNames = CacheConfig.DOMAIN_BY_HOST, key = "#host")
    public ResolvedTenant resolveEnabled(String host) {
        Domain domain = domainMapper.selectOne(new LambdaQueryWrapper<Domain>()
                .eq(Domain::getHost, host)
                .eq(Domain::getStatus, 1)
                .last("LIMIT 1"));
        if (domain == null) {
            throw new BusinessException(ErrorCode.DOMAIN_NOT_FOUND, "域名未绑定站点");
        }
        Tenant tenant = tenantMapper.selectById(domain.getTenantId());
        if (tenant == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        if (tenant.getStatus() == null || tenant.getStatus() != 1) {
            throw new BusinessException(ErrorCode.TENANT_DISABLED);
        }
        String role = StringUtils.hasText(domain.getRole()) ? domain.getRole().trim().toUpperCase() : "FORUM";
        if (!"ENTRY".equals(role)) {
            role = "FORUM";
        }
        return new ResolvedTenant(tenant.getId(), host, role);
    }

    @CacheEvict(cacheNames = {
            CacheConfig.DOMAIN_BY_HOST,
            CacheConfig.TENANT_CURRENT
    }, allEntries = true)
    public void evictAll() {
        // no-op: annotation-driven eviction
    }

    public record ResolvedTenant(Long tenantId, String host, String domainRole) {
    }
}
