package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.service.TenantQueryService;
import com.liuhecai.vo.DemoNoteVO;
import com.liuhecai.vo.TenantVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TenantController {

    private final TenantQueryService tenantQueryService;

    @GetMapping("/tenant/current")
    public Result<TenantVO> current(HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=30");
        response.setHeader("Vary", "Host, X-Forwarded-Host");
        return Result.ok(tenantQueryService.getCurrentTenant());
    }

    @GetMapping("/demo-notes")
    public Result<List<DemoNoteVO>> demoNotes() {
        return Result.ok(tenantQueryService.listCurrentDemoNotes());
    }
}
