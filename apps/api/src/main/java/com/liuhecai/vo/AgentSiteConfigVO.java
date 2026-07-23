package com.liuhecai.vo;

import lombok.Data;

@Data
public class AgentSiteConfigVO {
    private Long tenantId;
    private String name;
    private String announcement;
    private String kefuWechat;
    private String kefuQq;
    private String primaryColor;
    private String fontFamily;
    private String logoUrl;
    private String adBanner;
    private String themeJson;
}
