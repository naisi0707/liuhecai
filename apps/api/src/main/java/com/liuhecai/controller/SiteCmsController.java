package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.service.CmsService;
import com.liuhecai.service.TenantQueryService;
import com.liuhecai.vo.SiteMenuVO;
import com.liuhecai.vo.SitePageVO;
import com.liuhecai.vo.TenantDirectoryItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteCmsController {

    private final CmsService cmsService;
    private final TenantQueryService tenantQueryService;

    @GetMapping("/menus")
    public Result<List<SiteMenuVO>> menus() {
        return Result.ok(cmsService.listPublicMenus());
    }

    @GetMapping("/pages/{pageKey}")
    public Result<SitePageVO> page(@PathVariable String pageKey) {
        return Result.ok(cmsService.getPublicPage(pageKey));
    }

    @GetMapping("/tenants")
    public Result<List<TenantDirectoryItemVO>> otherTenants() {
        return Result.ok(tenantQueryService.listOtherTenants());
    }
}
