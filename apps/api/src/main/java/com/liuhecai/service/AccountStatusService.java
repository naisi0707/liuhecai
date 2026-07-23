package com.liuhecai.service;

import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.entity.AgentAccount;
import com.liuhecai.entity.SuperAdmin;
import com.liuhecai.entity.User;
import com.liuhecai.mapper.AgentAccountMapper;
import com.liuhecai.mapper.SuperAdminMapper;
import com.liuhecai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountStatusService {

    private final SuperAdminMapper superAdminMapper;
    private final AgentAccountMapper agentAccountMapper;
    private final UserMapper userMapper;

    public boolean isActive(AuthRealm realm, Long id) {
        return switch (realm) {
            case SUPER -> {
                SuperAdmin admin = superAdminMapper.selectById(id);
                yield admin != null && admin.getEnabled() != null && admin.getEnabled() == 1;
            }
            case AGENT -> {
                AgentAccount agent = agentAccountMapper.selectById(id);
                yield agent != null && agent.getEnabled() != null && agent.getEnabled() == 1;
            }
            case USER -> {
                User user = userMapper.selectById(id);
                yield user != null && user.getEnabled() != null && user.getEnabled() == 1;
            }
        };
    }
}
