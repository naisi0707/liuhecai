package com.liuhecai.service;

import com.liuhecai.dto.AgentSiteConfigRequest;
import com.liuhecai.vo.AgentSiteConfigVO;

public interface AgentSiteService {
    AgentSiteConfigVO getConfig();

    AgentSiteConfigVO updateConfig(AgentSiteConfigRequest request);
}
