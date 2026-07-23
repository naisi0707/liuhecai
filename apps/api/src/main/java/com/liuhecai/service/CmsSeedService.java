package com.liuhecai.service;

public interface CmsSeedService {
    /** 为租户写入刘伯温默认菜单与页面（若已有菜单则跳过） */
    void seedDefaultsIfEmpty(Long tenantId);

    /** 强制覆盖写入默认模板（仅开站内部用时可先清空再写） */
    void seedDefaults(Long tenantId);
}
