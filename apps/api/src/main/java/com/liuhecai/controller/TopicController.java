package com.liuhecai.controller;

import com.liuhecai.auth.AuthContext;
import com.liuhecai.auth.AuthUser;
import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.common.result.PageResult;
import com.liuhecai.common.result.Result;
import com.liuhecai.service.TopicService;
import com.liuhecai.vo.TopicVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public Result<PageResult<TopicVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(topicService.listPublic(viewerUserId(), page, size));
    }

    @GetMapping("/{id}")
    public Result<TopicVO> detail(@PathVariable Long id) {
        return Result.ok(topicService.detail(id, viewerUserId()));
    }

    private Long viewerUserId() {
        AuthUser user = AuthContext.get();
        if (user != null && user.getRealm() == AuthRealm.USER) {
            return user.getId();
        }
        return null;
    }
}
