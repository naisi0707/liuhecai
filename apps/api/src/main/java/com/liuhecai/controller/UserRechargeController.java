package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.dto.RechargeCreateRequest;
import com.liuhecai.service.RechargeService;
import com.liuhecai.vo.RechargeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/recharges")
@RequiredArgsConstructor
@Validated
public class UserRechargeController {

    private final RechargeService rechargeService;

    @PostMapping
    public Result<RechargeVO> create(@Valid @RequestBody RechargeCreateRequest request) {
        return Result.ok(rechargeService.create(request));
    }

    @GetMapping
    public Result<List<RechargeVO>> listMine() {
        return Result.ok(rechargeService.listMine());
    }
}
