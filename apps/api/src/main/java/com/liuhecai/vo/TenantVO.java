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
}
