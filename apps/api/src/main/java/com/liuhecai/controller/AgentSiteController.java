package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.dto.AgentSiteConfigRequest;
import com.liuhecai.service.AgentSiteService;
import com.liuhecai.vo.AgentSiteConfigVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/site-config")
@RequiredArgsConstructor
@Validated
public class AgentSiteController {

    private final AgentSiteService agentSiteService;

    @GetMapping
    public Result<AgentSiteConfigVO> get() {
        return Result.ok(agentSiteService.getConfig());
    }

    @PutMapping
    public Result<AgentSiteConfigVO> update(@Valid @RequestBody AgentSiteConfigRequest request) {
        return Result.ok(agentSiteService.updateConfig(request));
    }
}
