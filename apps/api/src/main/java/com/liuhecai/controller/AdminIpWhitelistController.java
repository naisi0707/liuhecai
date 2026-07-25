package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.dto.IpWhitelistUpdateRequest;
import com.liuhecai.service.IpWhitelistService;
import com.liuhecai.vo.IpWhitelistVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ip-whitelist")
@RequiredArgsConstructor
@Validated
public class AdminIpWhitelistController {

    private final IpWhitelistService ipWhitelistService;

    @GetMapping
    public Result<IpWhitelistVO> get(HttpServletRequest request) {
        return Result.ok(ipWhitelistService.getConfig(request));
    }

    @PutMapping
    public Result<IpWhitelistVO> put(@Valid @RequestBody IpWhitelistUpdateRequest body, HttpServletRequest request) {
        return Result.ok(ipWhitelistService.replace(body, request));
    }
}
