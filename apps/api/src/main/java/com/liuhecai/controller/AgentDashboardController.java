package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.service.AgentDashboardService;
import com.liuhecai.vo.AgentDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentDashboardController {

    private final AgentDashboardService agentDashboardService;

    @GetMapping("/dashboard")
    public Result<AgentDashboardVO> dashboard(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(agentDashboardService.getDashboard(days));
    }
}
