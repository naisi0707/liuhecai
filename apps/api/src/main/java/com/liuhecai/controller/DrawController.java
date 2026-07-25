package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.service.DrawService;
import com.liuhecai.vo.DrawHistoryItemVO;
import com.liuhecai.vo.DrawResultVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/draws")
@RequiredArgsConstructor
public class DrawController {

    private final DrawService drawService;

    @GetMapping("/latest")
    public Result<DrawResultVO> latest(
            @RequestParam(defaultValue = "MACAU_NEW") String lotteryType,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=10");
        return Result.ok(drawService.latest(lotteryType));
    }

    @GetMapping("/latest-all")
    public Result<List<DrawResultVO>> latestAll(HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=10");
        return Result.ok(drawService.latestAll());
    }

    @GetMapping("/history")
    public Result<List<DrawHistoryItemVO>> history(
            @RequestParam(defaultValue = "MACAU_NEW") String lotteryType,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "100") int pageSize,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=60");
        return Result.ok(drawService.history(lotteryType, year, pageSize));
    }
}
