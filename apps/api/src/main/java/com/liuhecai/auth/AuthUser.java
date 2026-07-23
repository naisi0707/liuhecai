package com.liuhecai.auth;

import com.liuhecai.common.enums.AuthRealm;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUser {
    private Long id;
    private String username;
    private AuthRealm realm;
    private Long tenantId;
    private Integer tokenVersion;
}
