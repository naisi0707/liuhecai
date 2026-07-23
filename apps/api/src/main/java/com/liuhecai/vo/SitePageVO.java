package com.liuhecai.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class SitePageVO {
    private String id;
    private String pageKey;
    private String title;
    private JsonNode content;
}
