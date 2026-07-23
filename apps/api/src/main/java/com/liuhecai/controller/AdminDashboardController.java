package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.service.AdminDashboardService;
import com.liuhecai.vo.AdminDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> dashboard(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(adminDashboardService.getDashboard(days));
    }
}
