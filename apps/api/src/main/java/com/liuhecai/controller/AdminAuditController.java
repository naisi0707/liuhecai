package com.liuhecai.controller;

import com.liuhecai.common.result.PageResult;
import com.liuhecai.common.result.Result;
import com.liuhecai.service.AdminAuditService;
import com.liuhecai.vo.OpAuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Validated
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    public Result<PageResult<OpAuditLogVO>> page(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminAuditService.page(action, operatorName, targetType, page, size));
    }
}
