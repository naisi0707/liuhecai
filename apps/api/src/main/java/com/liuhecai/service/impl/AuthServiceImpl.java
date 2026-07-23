package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.auth.AuthContext;
import com.liuhecai.auth.AuthUser;
import com.liuhecai.auth.JwtService;
import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.dto.LoginRequest;
import com.liuhecai.dto.UserRegisterRequest;
import com.liuhecai.entity.AgentAccount;
import com.liuhecai.entity.SuperAdmin;
import com.liuhecai.entity.User;
import com.liuhecai.mapper.AgentAccountMapper;
import com.liuhecai.mapper.SuperAdminMapper;
import com.liuhecai.mapper.UserMapper;
import com.liuhecai.service.AuthService;
import com.liuhecai.tenant.TenantContext;
import com.liuhecai.vo.AuthProfileVO;
import com.liuhecai.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SuperAdminMapper superAdminMapper;
    private final AgentAccountMapper agentAccountMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginVO loginSuper(LoginRequest request) {
        SuperAdmin admin = superAdminMapper.selectOne(new LambdaQueryWrapper<SuperAdmin>()
                .eq(SuperAdmin::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        if (admin.getEnabled() == null || admin.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        return toLoginVO(new AuthUser(admin.getId(), admin.getUsername(), AuthRealm.SUPER, null,
                admin.getTokenVersion()));
    }

    @Override
    public LoginVO loginAgent(LoginRequest request) {
        AgentAccount agent = agentAccountMapper.selectOne(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (agent == null || !passwordEncoder.matches(request.getPassword(), agent.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        if (agent.getEnabled() == null || agent.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        return toLoginVO(new AuthUser(agent.getId(), agent.getUsername(), AuthRealm.AGENT, agent.getTenantId(),
                agent.getTokenVersion()));
    }

    @Override
    public LoginVO loginUser(LoginRequest request) {
        Long tenantId = requireTenantId();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!tenantId.equals(user.getTenantId())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        return toLoginVO(new AuthUser(user.getId(), user.getUsername(), AuthRealm.USER, user.getTenantId(),
                user.getTokenVersion()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO registerUser(UserRegisterRequest request) {
        Long tenantId = requireTenantId();
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "注册失败，请更换用户名或稍后重试");
        }
        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCoinBalance(0);
        user.setEnabled(1);
        userMapper.insert(user);
        return toLoginVO(new AuthUser(user.getId(), user.getUsername(), AuthRealm.USER, tenantId, 0));
    }

    @Override
    public AuthProfileVO currentProfile() {
        AuthUser authUser = AuthContext.get();
        if (authUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        AuthProfileVO vo = new AuthProfileVO();
        vo.setId(authUser.getId());
        vo.setUsername(authUser.getUsername());
        vo.setRealm(authUser.getRealm().name());
        vo.setTenantId(authUser.getTenantId());
        if (authUser.getRealm() == AuthRealm.USER) {
            User user = userMapper.selectById(authUser.getId());
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            vo.setCoinBalance(user.getCoinBalance());
        }
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.DOMAIN_NOT_FOUND);
        }
        return tenantId;
    }

    private LoginVO toLoginVO(AuthUser user) {
        LoginVO vo = new LoginVO();
        vo.setToken(jwtService.createToken(user));
        vo.setRealm(user.getRealm().name());
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setTenantId(user.getTenantId());
        return vo;
    }
}
