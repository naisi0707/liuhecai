package com.liuhecai.service;

import com.liuhecai.common.enums.AuthRealm;

public interface TokenVersionService {

    int currentVersion(AuthRealm realm, Long id);

    void bump(AuthRealm realm, Long id);
}
