package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.service.CmsService;
import com.liuhecai.service.TenantQueryService;
import com.liuhecai.vo.SiteMenuVO;
import com.liuhecai.vo.SitePageVO;
import com.liuhecai.vo.TenantDirectoryItemVO;
import jakarta.servlet.http.HttpServletResponse;
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
    public Result<List<SiteMenuVO>> menus(HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=30");
        return Result.ok(cmsService.listPublicMenus());
    }

    @GetMapping("/pages/{pageKey}")
    public Result<SitePageVO> page(@PathVariable String pageKey, HttpServletResponse response) {
        // rules 等公开页可短缓存；home 也相对稳定
        response.setHeader("Cache-Control", "public, max-age=30");
        return Result.ok(cmsService.getPublicPage(pageKey));
    }

    @GetMapping("/tenants")
    public Result<List<TenantDirectoryItemVO>> otherTenants() {
        return Result.ok(tenantQueryService.listOtherTenants());
    }
}
