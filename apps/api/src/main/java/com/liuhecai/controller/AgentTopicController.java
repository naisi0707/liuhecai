package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.dto.TopicCreateRequest;
import com.liuhecai.dto.TopicStatusRequest;
import com.liuhecai.dto.CoinGrantRequest;
import com.liuhecai.service.TopicService;
import com.liuhecai.vo.TopicVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Validated
public class AgentTopicController {

    private final TopicService topicService;

    @PostMapping("/topics")
    public Result<TopicVO> create(@Valid @RequestBody TopicCreateRequest request) {
        return Result.ok(topicService.createByAgent(request));
    }

    @GetMapping("/topics")
    public Result<List<TopicVO>> list() {
        return Result.ok(topicService.listForAgent());
    }

    @PutMapping("/topics/{id}/status")
    public Result<TopicVO> updateStatus(@PathVariable Long id, @Valid @RequestBody TopicStatusRequest request) {
        return Result.ok(topicService.updateStatus(id, request));
    }

    @PostMapping("/coins/grant")
    public Result<Map<String, Integer>> grant(@Valid @RequestBody CoinGrantRequest request) {
        Integer balance = topicService.grantCoins(request);
        return Result.ok(Map.of("coinBalance", balance));
    }
}
