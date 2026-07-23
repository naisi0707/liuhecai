package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.dto.RechargeRejectRequest;
import com.liuhecai.service.RechargeService;
import com.liuhecai.vo.RechargeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent/recharges")
@RequiredArgsConstructor
@Validated
public class AgentRechargeController {

    private final RechargeService rechargeService;

    @GetMapping
    public Result<List<RechargeVO>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(rechargeService.listForAgent(status));
    }

    @PostMapping("/{id}/approve")
    public Result<RechargeVO> approve(@PathVariable Long id) {
        return Result.ok(rechargeService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public Result<RechargeVO> reject(@PathVariable Long id, @Valid @RequestBody(required = false) RechargeRejectRequest request) {
        return Result.ok(rechargeService.reject(id, request == null ? new RechargeRejectRequest() : request));
    }
}
