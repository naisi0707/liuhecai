package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.common.util.PasswordGenerator;
import com.liuhecai.dto.AgentCreateRequest;
import com.liuhecai.dto.DomainBindRequest;
import com.liuhecai.dto.TenantCreateRequest;
import com.liuhecai.dto.TenantStatusRequest;
import com.liuhecai.entity.AgentAccount;
import com.liuhecai.entity.Domain;
import com.liuhecai.entity.Tenant;
import com.liuhecai.mapper.AgentAccountMapper;
import com.liuhecai.mapper.DomainMapper;
import com.liuhecai.mapper.TenantMapper;
import com.liuhecai.service.CmsSeedService;
import com.liuhecai.service.TenantAdminService;
import com.liuhecai.service.TokenVersionService;
import com.liuhecai.tenant.DomainTenantLookup;
import com.liuhecai.vo.AgentAdminVO;
import com.liuhecai.vo.DomainAdminVO;
import com.liuhecai.vo.TenantAdminVO;
import com.liuhecai.vo.TenantCreateResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantAdminServiceImpl implements TenantAdminService {

    private static final String DEFAULT_THEME =
            "{\"primaryColor\":\"#c62828\",\"fontFamily\":\"Microsoft YaHei\"}";

    private final TenantMapper tenantMapper;
    private final DomainMapper domainMapper;
    private final AgentAccountMapper agentAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final CmsSeedService cmsSeedService;
    private final TokenVersionService tokenVersionService;
    private final DomainTenantLookup domainTenantLookup;

    @Override
    public List<TenantAdminVO> listTenants() {
        List<Tenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .orderByAsc(Tenant::getId));
        if (tenants.isEmpty()) {
            return List.of();
        }
        List<Long> tenantIds = tenants.stream().map(Tenant::getId).toList();
        Map<Long, List<Domain>> domainsByTenant = domainMapper.selectList(new LambdaQueryWrapper<Domain>()
                        .in(Domain::getTenantId, tenantIds)
                        .orderByDesc(Domain::getIsPrimary)
                        .orderByAsc(Domain::getId))
                .stream()
                .collect(Collectors.groupingBy(Domain::getTenantId));
        Map<Long, List<AgentAccount>> agentsByTenant = agentAccountMapper.selectList(
                        new LambdaQueryWrapper<AgentAccount>()
                                .in(AgentAccount::getTenantId, tenantIds)
                                .orderByAsc(AgentAccount::getId))
                .stream()
                .collect(Collectors.groupingBy(AgentAccount::getTenantId));
        List<TenantAdminVO> result = new ArrayList<>(tenants.size());
        for (Tenant tenant : tenants) {
            result.add(toDetail(
                    tenant,
                    domainsByTenant.getOrDefault(tenant.getId(), List.of()),
                    agentsByTenant.getOrDefault(tenant.getId(), List.of())));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantCreateResultVO createTenant(TenantCreateRequest request) {
        String host = normalizeHost(request.getPrimaryHost());
        assertHostAvailable(host);

        Tenant tenant = new Tenant();
        tenant.setName(request.getName().trim());
        tenant.setStatus(1);
        tenant.setThemeJson(DEFAULT_THEME);
        tenant.setAnnouncement(StringUtils.hasText(request.getAnnouncement())
                ? request.getAnnouncement().trim()
                : "欢迎来到 " + request.getName().trim());
        tenantMapper.insert(tenant);

        Domain domain = new Domain();
        domain.setTenantId(tenant.getId());
        domain.setHost(host);
        domain.setIsPrimary(1);
        domain.setRole("FORUM");
        domain.setStatus(1);
        domainMapper.insert(domain);

        String agentUsername = StringUtils.hasText(request.getAgentUsername())
                ? request.getAgentUsername().trim()
                : "agent";
        AgentAdminVO agent = createAgentInternal(tenant.getId(), agentUsername);
        cmsSeedService.seedDefaults(tenant.getId());
        domainTenantLookup.evictAll();

        TenantCreateResultVO result = new TenantCreateResultVO();
        result.setTenant(toDetail(requireTenant(tenant.getId())));
        result.setAgent(agent);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DomainAdminVO bindDomain(Long tenantId, DomainBindRequest request) {
        Tenant tenant = requireTenant(tenantId);
        String host = normalizeHost(request.getHost());
        assertHostAvailable(host);

        Domain domain = new Domain();
        domain.setTenantId(tenant.getId());
        domain.setHost(host);
        domain.setIsPrimary(request.getIsPrimary() != null && request.getIsPrimary() == 1 ? 1 : 0);
        domain.setRole(normalizeRole(request.getRole()));
        domain.setStatus(1);
        domainMapper.insert(domain);
        domainTenantLookup.evictAll();
        return toDomainVO(domain);
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "FORUM";
        }
        String value = role.trim().toUpperCase();
        if ("ENTRY".equals(value)) {
            return "ENTRY";
        }
        if ("FORUM".equals(value)) {
            return "FORUM";
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "role 只能为 ENTRY 或 FORUM");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantAdminVO updateStatus(Long tenantId, TenantStatusRequest request) {
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 只能为 0 或 1");
        }
        Tenant tenant = requireTenant(tenantId);
        tenant.setStatus(request.getStatus());
        tenantMapper.updateById(tenant);
        domainTenantLookup.evictAll();
        return toDetail(requireTenant(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentAdminVO createAgent(Long tenantId, AgentCreateRequest request) {
        requireTenant(tenantId);
        return createAgentInternal(tenantId, request.getUsername().trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentAdminVO resetAgentPassword(Long agentId) {
        AgentAccount agent = agentAccountMapper.selectById(agentId);
        if (agent == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "代理账号不存在");
        }
        String raw = PasswordGenerator.randomPassword(10);
        agent.setPasswordHash(passwordEncoder.encode(raw));
        agentAccountMapper.updateById(agent);
        tokenVersionService.bump(AuthRealm.AGENT, agentId);
        AgentAdminVO vo = toAgentVO(agent);
        vo.setRawPassword(raw);
        return vo;
    }

    private AgentAdminVO createAgentInternal(Long tenantId, String username) {
        Long count = agentAccountMapper.selectCount(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getTenantId, tenantId)
                .eq(AgentAccount::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.AGENT_USERNAME_EXISTS);
        }
        String raw = PasswordGenerator.randomPassword(10);
        AgentAccount agent = new AgentAccount();
        agent.setTenantId(tenantId);
        agent.setUsername(username);
        agent.setPasswordHash(passwordEncoder.encode(raw));
        agent.setEnabled(1);
        agentAccountMapper.insert(agent);
        AgentAdminVO vo = toAgentVO(agent);
        vo.setRawPassword(raw);
        return vo;
    }

    private Tenant requireTenant(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    private void assertHostAvailable(String host) {
        Long count = domainMapper.selectCount(new LambdaQueryWrapper<Domain>()
                .eq(Domain::getHost, host));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.DOMAIN_EXISTS);
        }
    }

    private String normalizeHost(String host) {
        String value = host.trim().toLowerCase();
        int colon = value.indexOf(':');
        if (colon > -1) {
            value = value.substring(0, colon);
        }
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "域名不能为空");
        }
        return value;
    }

    private TenantAdminVO toDetail(Tenant tenant) {
        List<Domain> domains = domainMapper.selectList(new LambdaQueryWrapper<Domain>()
                .eq(Domain::getTenantId, tenant.getId())
                .orderByDesc(Domain::getIsPrimary)
                .orderByAsc(Domain::getId));
        List<AgentAccount> agents = agentAccountMapper.selectList(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getTenantId, tenant.getId())
                .orderByAsc(AgentAccount::getId));
        return toDetail(tenant, domains, agents);
    }

    private TenantAdminVO toDetail(Tenant tenant, List<Domain> domains, List<AgentAccount> agents) {
        TenantAdminVO vo = new TenantAdminVO();
        vo.setId(tenant.getId());
        vo.setName(tenant.getName());
        vo.setStatus(tenant.getStatus());
        vo.setAnnouncement(tenant.getAnnouncement());
        vo.setKefuWechat(tenant.getKefuWechat());
        vo.setDomains(domains.stream().map(this::toDomainVO).toList());
        vo.setAgents(agents.stream().map(this::toAgentVO).toList());
        return vo;
    }

    private DomainAdminVO toDomainVO(Domain domain) {
        DomainAdminVO vo = new DomainAdminVO();
        vo.setId(domain.getId());
        vo.setHost(domain.getHost());
        vo.setIsPrimary(domain.getIsPrimary());
        vo.setRole(StringUtils.hasText(domain.getRole()) ? domain.getRole() : "FORUM");
        vo.setStatus(domain.getStatus());
        return vo;
    }

    private AgentAdminVO toAgentVO(AgentAccount agent) {
        AgentAdminVO vo = new AgentAdminVO();
        vo.setId(agent.getId());
        vo.setUsername(agent.getUsername());
        vo.setEnabled(agent.getEnabled());
        return vo;
    }
}
