package com.liuhecai.service;

import com.liuhecai.dto.IpWhitelistUpdateRequest;
import com.liuhecai.vo.IpWhitelistVO;
import jakarta.servlet.http.HttpServletRequest;

public interface IpWhitelistService {

    /** 是否放行该请求（未启用白名单时恒为 true） */
    boolean isAllowed(HttpServletRequest request);

    IpWhitelistVO getConfig(HttpServletRequest request);

    IpWhitelistVO replace(IpWhitelistUpdateRequest request, HttpServletRequest httpRequest);

    void invalidateCache();
}
