package com.liuhecai.vo;

import lombok.Data;

@Data
public class SiteMenuVO {
    private String id;
    private String code;
    private String title;
    private String path;
    private Integer sortNo;
    private Integer visible;
}
