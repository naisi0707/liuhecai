package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.dto.AgentCreateRequest;
import com.liuhecai.dto.DomainBindRequest;
import com.liuhecai.dto.TenantCreateRequest;
import com.liuhecai.dto.TenantStatusRequest;
import com.liuhecai.service.TenantAdminService;
import com.liuhecai.vo.AgentAdminVO;
import com.liuhecai.vo.DomainAdminVO;
import com.liuhecai.vo.TenantAdminVO;
import com.liuhecai.vo.TenantCreateResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;

    @GetMapping("/tenants")
    public Result<List<TenantAdminVO>> list() {
        return Result.ok(tenantAdminService.listTenants());
    }

    @PostMapping("/tenants")
    public Result<TenantCreateResultVO> create(@Valid @RequestBody TenantCreateRequest request) {
        return Result.ok(tenantAdminService.createTenant(request));
    }

    @PostMapping("/tenants/{tenantId}/domains")
    public Result<DomainAdminVO> bindDomain(@PathVariable Long tenantId,
                                            @Valid @RequestBody DomainBindRequest request) {
        return Result.ok(tenantAdminService.bindDomain(tenantId, request));
    }

    @PutMapping("/tenants/{tenantId}/status")
    public Result<TenantAdminVO> updateStatus(@PathVariable Long tenantId,
                                              @Valid @RequestBody TenantStatusRequest request) {
        return Result.ok(tenantAdminService.updateStatus(tenantId, request));
    }

    @PostMapping("/tenants/{tenantId}/agents")
    public Result<AgentAdminVO> createAgent(@PathVariable Long tenantId,
                                            @Valid @RequestBody AgentCreateRequest request) {
        return Result.ok(tenantAdminService.createAgent(tenantId, request));
    }
}
