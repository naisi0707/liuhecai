package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.dto.EntryLinesSaveRequest;
import com.liuhecai.service.EntryLineAdminService;
import com.liuhecai.vo.EntryLineAdminVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/entry-domains")
@RequiredArgsConstructor
@Validated
public class EntryLineAdminController {

    private final EntryLineAdminService entryLineAdminService;

    @GetMapping("/{domainId}/lines")
    public Result<List<EntryLineAdminVO>> list(@PathVariable Long domainId) {
        return Result.ok(entryLineAdminService.listByDomainId(domainId));
    }

    @PutMapping("/{domainId}/lines")
    public Result<List<EntryLineAdminVO>> save(@PathVariable Long domainId,
                                               @Valid @RequestBody EntryLinesSaveRequest request) {
        return Result.ok(entryLineAdminService.replaceLines(domainId, request));
    }
}
