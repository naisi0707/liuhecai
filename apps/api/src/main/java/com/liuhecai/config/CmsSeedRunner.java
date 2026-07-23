package com.liuhecai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.entity.Tenant;
import com.liuhecai.mapper.TenantMapper;
import com.liuhecai.service.CmsSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class CmsSeedRunner implements ApplicationRunner {

    private final TenantMapper tenantMapper;
    private final CmsSeedService cmsSeedService;

    @Override
    public void run(ApplicationArguments args) {
        List<Tenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>().orderByAsc(Tenant::getId));
        for (Tenant t : tenants) {
            try {
                cmsSeedService.seedDefaultsIfEmpty(t.getId());
            } catch (Exception e) {
                log.warn("CMS seed skip tenant {}: {}", t.getId(), e.getMessage());
            }
        }
        log.info("CMS seed checked for {} tenants", tenants.size());
    }
}
