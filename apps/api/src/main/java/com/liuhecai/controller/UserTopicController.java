package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.service.TopicService;
import com.liuhecai.vo.PurchaseResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/topics")
@RequiredArgsConstructor
public class UserTopicController {

    private final TopicService topicService;

    @PostMapping("/{id}/purchase")
    public Result<PurchaseResultVO> purchase(@PathVariable Long id) {
        return Result.ok(topicService.purchase(id));
    }
}
