package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.dto.DrawOverrideRequest;
import com.liuhecai.service.DrawService;
import com.liuhecai.vo.DrawResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/draws")
@RequiredArgsConstructor
@Validated
public class AgentDrawController {

    private final DrawService drawService;

    @GetMapping("/latest")
    public Result<DrawResultVO> latest(@RequestParam(defaultValue = "MACAU_NEW") String lotteryType) {
        return Result.ok(drawService.latest(lotteryType));
    }

    @GetMapping("/latest-all")
    public Result<List<DrawResultVO>> latestAll() {
        return Result.ok(drawService.latestAll());
    }

    @PostMapping("/fetch")
    public Result<Map<String, Object>> fetch() {
        return Result.ok(drawService.fetchAll());
    }

    @PostMapping("/override")
    public Result<DrawResultVO> override(@Valid @RequestBody DrawOverrideRequest request) {
        return Result.ok(drawService.override(request));
    }
}
