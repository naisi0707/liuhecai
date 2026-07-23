package com.liuhecai.service;

public interface HtmlSanitizeService {
    /** 清洗富文本 HTML；null/空白原样返回 */
    String sanitize(String html);

    /** 校验可安全用于 img/iframe 的 URL（http(s) 或站点相对路径） */
    boolean isSafeResourceUrl(String url);

    /** 不安全则抛业务异常或返回清洗后的空串；空允许 */
    String requireSafeResourceUrl(String url, String fieldLabel);
}
