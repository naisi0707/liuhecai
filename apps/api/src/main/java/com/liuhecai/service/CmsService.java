package com.liuhecai.service;

import com.liuhecai.dto.CmsMenusSaveRequest;
import com.liuhecai.dto.CmsPageSaveRequest;
import com.liuhecai.vo.SiteMenuVO;
import com.liuhecai.vo.SitePageVO;

import java.util.List;

public interface CmsService {
    List<SiteMenuVO> listPublicMenus();

    SitePageVO getPublicPage(String pageKey);

    List<SiteMenuVO> listAgentMenus();

    List<SiteMenuVO> saveAgentMenus(CmsMenusSaveRequest request);

    List<SitePageVO> listAgentPages();

    SitePageVO getAgentPage(String pageKey);

    SitePageVO saveAgentPage(String pageKey, CmsPageSaveRequest request);
}
