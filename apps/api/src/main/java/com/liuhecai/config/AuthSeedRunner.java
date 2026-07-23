package com.liuhecai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.entity.AgentAccount;
import com.liuhecai.entity.SuperAdmin;
import com.liuhecai.mapper.AgentAccountMapper;
import com.liuhecai.mapper.SuperAdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthSeedRunner implements ApplicationRunner {

    private final SuperAdminMapper superAdminMapper;
    private final AgentAccountMapper agentAccountMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedSuper("admin", "admin123");
        seedAgent(1001L, "agent_a", "agent123");
        seedAgent(1002L, "agent_b", "agent123");
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

    private void seedAgent(Long tenantId, String username, String rawPassword) {
        Long count = agentAccountMapper.selectCount(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getTenantId, tenantId)
                .eq(AgentAccount::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        AgentAccount agent = new AgentAccount();
        agent.setTenantId(tenantId);
        agent.setUsername(username);
        agent.setPasswordHash(passwordEncoder.encode(rawPassword));
        agent.setEnabled(1);
        agentAccountMapper.insert(agent);
        log.info("Seeded agent: {} tenant={}", username, tenantId);
    }
}
