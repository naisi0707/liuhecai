package com.liuhecai.controller;

import com.liuhecai.auth.AuthContext;
import com.liuhecai.auth.AuthUser;
import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.common.result.Result;
import com.liuhecai.service.TopicService;
import com.liuhecai.vo.TopicVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public Result<List<TopicVO>> list() {
        return Result.ok(topicService.listPublic(viewerUserId()));
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
