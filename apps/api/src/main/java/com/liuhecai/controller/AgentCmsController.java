package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.dto.CmsMenusSaveRequest;
import com.liuhecai.dto.CmsPageSaveRequest;
import com.liuhecai.service.CmsService;
import com.liuhecai.vo.SiteMenuVO;
import com.liuhecai.vo.SitePageVO;
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
@RequestMapping("/api/agent/cms")
@RequiredArgsConstructor
@Validated
public class AgentCmsController {

    private final CmsService cmsService;

    @GetMapping("/menus")
    public Result<List<SiteMenuVO>> listMenus() {
        return Result.ok(cmsService.listAgentMenus());
    }

    @PutMapping("/menus")
    public Result<List<SiteMenuVO>> saveMenus(@Valid @RequestBody CmsMenusSaveRequest request) {
        return Result.ok(cmsService.saveAgentMenus(request));
    }

    @GetMapping("/pages")
    public Result<List<SitePageVO>> listPages() {
        return Result.ok(cmsService.listAgentPages());
    }

    @GetMapping("/pages/{pageKey}")
    public Result<SitePageVO> getPage(@PathVariable String pageKey) {
        return Result.ok(cmsService.getAgentPage(pageKey));
    }

    @PutMapping("/pages/{pageKey}")
    public Result<SitePageVO> savePage(@PathVariable String pageKey,
                                       @Valid @RequestBody CmsPageSaveRequest request) {
        return Result.ok(cmsService.saveAgentPage(pageKey, request));
    }
}
