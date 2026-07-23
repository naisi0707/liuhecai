package com.liuhecai.service.impl;

import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.mapper.AdminUserAgentMapper;
import com.liuhecai.mapper.TokenVersionMapper;
import com.liuhecai.service.TokenVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenVersionServiceImpl implements TokenVersionService {

    private final AdminUserAgentMapper adminUserAgentMapper;
    private final TokenVersionMapper tokenVersionMapper;

    @Override
    public int currentVersion(AuthRealm realm, Long id) {
        Integer version = switch (realm) {
            case USER -> adminUserAgentMapper.selectUserTokenVersion(id);
            case AGENT -> adminUserAgentMapper.selectAgentTokenVersion(id);
            case SUPER -> tokenVersionMapper.selectSuperTokenVersion(id);
        };
        return version == null ? 0 : version;
    }

    @Override
    public void bump(AuthRealm realm, Long id) {
        switch (realm) {
            case USER -> adminUserAgentMapper.bumpUserTokenVersion(id);
            case AGENT -> adminUserAgentMapper.bumpAgentTokenVersion(id);
            case SUPER -> tokenVersionMapper.bumpSuperTokenVersion(id);
        }
    }
}
