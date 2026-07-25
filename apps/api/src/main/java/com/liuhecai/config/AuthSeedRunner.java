package com.liuhecai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.entity.AgentAccount;
import com.liuhecai.entity.SuperAdmin;
import com.liuhecai.entity.Tenant;
import com.liuhecai.mapper.AgentAccountMapper;
import com.liuhecai.mapper.SuperAdminMapper;
import com.liuhecai.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class AuthSeedRunner implements ApplicationRunner {

    private static final Map<Long, String> DEFAULT_PRIMARY_USERNAMES = new LinkedHashMap<>();

    static {
        DEFAULT_PRIMARY_USERNAMES.put(1001L, "agent_a");
        DEFAULT_PRIMARY_USERNAMES.put(1002L, "agent_b");
        DEFAULT_PRIMARY_USERNAMES.put(1003L, "agent_ssz");
        DEFAULT_PRIMARY_USERNAMES.put(1004L, "agent_zcb");
        DEFAULT_PRIMARY_USERNAMES.put(1005L, "agent_rhfg");
    }

    private static final String DEMO_AGENT_PASSWORD = "agent123";

    private final SuperAdminMapper superAdminMapper;
    private final AgentAccountMapper agentAccountMapper;
    private final TenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        seedSuper("admin", "admin123");
        ensurePrimaryAgentsForAllTenants();
    }

    private void seedSuper(String username, String rawPassword) {
        Long count = superAdminMapper.selectCount(new LambdaQueryWrapper<SuperAdmin>()
                .eq(SuperAdmin::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        SuperAdmin admin = new SuperAdmin();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setEnabled(1);
        superAdminMapper.insert(admin);
        log.info("Seeded super admin: {}", username);
    }

    private void ensurePrimaryAgentsForAllTenants() {
        List<Tenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>().orderByAsc(Tenant::getId));
        for (Tenant tenant : tenants) {
            ensurePrimaryAgent(tenant);
        }
        log.info("Primary agents ensured for {} tenants", tenants.size());
    }

    private void ensurePrimaryAgent(Tenant tenant) {
        Long tenantId = tenant.getId();
        AgentAccount primary = null;
        if (tenant.getPrimaryAgentId() != null) {
            primary = agentAccountMapper.selectById(tenant.getPrimaryAgentId());
            if (primary != null && !tenantId.equals(primary.getTenantId())) {
                primary = null;
            }
        }
        if (primary == null) {
            primary = agentAccountMapper.selectOne(new LambdaQueryWrapper<AgentAccount>()
                    .eq(AgentAccount::getTenantId, tenantId)
                    .eq(AgentAccount::getIsPrimary, 1)
                    .last("LIMIT 1"));
        }
        if (primary == null) {
            primary = agentAccountMapper.selectOne(new LambdaQueryWrapper<AgentAccount>()
                    .eq(AgentAccount::getTenantId, tenantId)
                    .orderByAsc(AgentAccount::getId)
                    .last("LIMIT 1"));
        }
        if (primary == null) {
            String username = DEFAULT_PRIMARY_USERNAMES.getOrDefault(tenantId, "agent_" + tenantId);
            primary = findOrCreateAgent(tenantId, username, DEMO_AGENT_PASSWORD, true);
        } else {
            markPrimary(primary);
        }
        if (tenant.getPrimaryAgentId() == null || !tenant.getPrimaryAgentId().equals(primary.getId())) {
            tenant.setPrimaryAgentId(primary.getId());
            tenantMapper.updateById(tenant);
        }
    }

    private AgentAccount findOrCreateAgent(Long tenantId, String username, String rawPassword, boolean primary) {
        AgentAccount existing = agentAccountMapper.selectOne(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getTenantId, tenantId)
                .eq(AgentAccount::getUsername, username)
                .last("LIMIT 1"));
        if (existing != null) {
            if (primary) {
                markPrimary(existing);
            }
            return existing;
        }
        AgentAccount agent = new AgentAccount();
        agent.setTenantId(tenantId);
        agent.setUsername(username);
        agent.setPasswordHash(passwordEncoder.encode(rawPassword));
        agent.setEnabled(1);
        if (primary) {
            agent.setIsPrimary(1);
            agent.setPrimaryKey(1);
        } else {
            agent.setIsPrimary(0);
            agent.setPrimaryKey(null);
        }
        agentAccountMapper.insert(agent);
        log.info("Seeded agent: {} tenant={} primary={}", username, tenantId, primary);
        return agent;
    }

    private void markPrimary(AgentAccount agent) {
        if (agent.getIsPrimary() != null && agent.getIsPrimary() == 1
                && agent.getPrimaryKey() != null && agent.getPrimaryKey() == 1) {
            return;
        }
        // clear other primaries for tenant
        List<AgentAccount> others = agentAccountMapper.selectList(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getTenantId, agent.getTenantId())
                .ne(AgentAccount::getId, agent.getId())
                .and(w -> w.eq(AgentAccount::getIsPrimary, 1).or().eq(AgentAccount::getPrimaryKey, 1)));
        for (AgentAccount other : others) {
            other.setIsPrimary(0);
            other.setPrimaryKey(null);
            agentAccountMapper.updateById(other);
        }
        agent.setIsPrimary(1);
        agent.setPrimaryKey(1);
        agentAccountMapper.updateById(agent);
    }
}
