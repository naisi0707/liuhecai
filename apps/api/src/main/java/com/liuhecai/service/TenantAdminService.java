package com.liuhecai.service;

import com.liuhecai.dto.AgentCreateRequest;
import com.liuhecai.dto.DomainBindRequest;
import com.liuhecai.dto.TenantCreateRequest;
import com.liuhecai.dto.TenantStatusRequest;
import com.liuhecai.vo.AgentAdminVO;
import com.liuhecai.vo.DomainAdminVO;
import com.liuhecai.vo.TenantAdminVO;
import com.liuhecai.vo.TenantCreateResultVO;

import java.util.List;

public interface TenantAdminService {
    List<TenantAdminVO> listTenants();

    TenantCreateResultVO createTenant(TenantCreateRequest request);

    DomainAdminVO bindDomain(Long tenantId, DomainBindRequest request);

    TenantAdminVO updateStatus(Long tenantId, TenantStatusRequest request);

    AgentAdminVO createAgent(Long tenantId, AgentCreateRequest request);

    AgentAdminVO resetAgentPassword(Long agentId);
}
