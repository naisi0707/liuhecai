package com.liuhecai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "liuhecai.upload")
public class UploadProperties {
    /** 本地上传根目录 */
    private String root = "./uploads";
    /** 单文件最大字节数 */
    private long maxBytes = 2 * 1024 * 1024L;
}
