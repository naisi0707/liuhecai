package com.liuhecai.vo;

import lombok.Data;

@Data
public class TenantVO {
    private Long id;
    private String name;
    private Integer status;
    private String themeJson;
    private String primaryColor;
    private String fontFamily;
    private String logoUrl;
    private String adBanner;
    private String kefuWechat;
    private String kefuQq;
    private String announcement;
    private String host;
    /** ENTRY / FORUM */
    private String domainRole;
    /** 同租户论坛 Host，供入口跳转 */
    private String forumHost;
}
