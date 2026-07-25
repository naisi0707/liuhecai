package com.liuhecai.controller;

import com.liuhecai.common.result.PageResult;
import com.liuhecai.common.result.Result;
import com.liuhecai.common.util.CsvWriter;
import com.liuhecai.dto.BatchEnabledRequest;
import com.liuhecai.dto.EnabledRequest;
import com.liuhecai.service.AdminAgentService;
import com.liuhecai.vo.AdminAgentDetailVO;
import com.liuhecai.vo.AdminAgentListItemVO;
import com.liuhecai.vo.AgentAdminVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/agents")
@RequiredArgsConstructor
@Validated
public class AdminAgentController {

    private final AdminAgentService adminAgentService;

    @GetMapping
    public Result<PageResult<AdminAgentListItemVO>> page(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminAgentService.page(tenantId, username, enabled, page, size));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer enabled) {
        String csv = adminAgentService.exportAgentsCsv(tenantId, username, enabled);
        byte[] body = CsvWriter.toUtf8BomBytes(csv);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"agents.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    @GetMapping("/{id}")
    public Result<AdminAgentDetailVO> detail(@PathVariable Long id) {
        return Result.ok(adminAgentService.getDetail(id));
    }

    @PutMapping("/{id}/enabled")
    public Result<AdminAgentListItemVO> updateEnabled(@PathVariable Long id,
                                                      @Valid @RequestBody EnabledRequest request) {
        return Result.ok(adminAgentService.updateEnabled(id, request));
    }

    @PostMapping("/{id}/reset-password")
    public Result<AgentAdminVO> resetPassword(@PathVariable Long id) {
        return Result.ok(adminAgentService.resetPassword(id));
    }

    @PostMapping("/{id}/force-logout")
    public Result<Void> forceLogout(@PathVariable Long id) {
        adminAgentService.forceLogout(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/delete")
    public Result<Void> softDelete(@PathVariable Long id) {
        adminAgentService.softDelete(id);
        return Result.ok(null);
    }

    @PostMapping("/batch-enabled")
    public Result<Void> batchEnabled(@Valid @RequestBody BatchEnabledRequest request) {
        adminAgentService.batchUpdateEnabled(request.getIds(), request.getEnabled());
        return Result.ok(null);
    }
}
