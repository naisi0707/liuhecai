package com.liuhecai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentSiteConfigRequest {
    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 512)
    private String announcement;

    @Size(max = 64)
    private String kefuWechat;

    @Size(max = 32)
    private String kefuQq;

    @Size(max = 32)
    private String primaryColor;

    @Size(max = 64)
    private String fontFamily;

    @Size(max = 512)
    private String logoUrl;

    @Size(max = 256)
    private String adBanner;
}
