package com.liuhecai.service;

import com.liuhecai.vo.DemoNoteVO;
import com.liuhecai.vo.TenantDirectoryItemVO;
import com.liuhecai.vo.TenantVO;

import java.util.List;

public interface TenantQueryService {
    TenantVO getCurrentTenant();

    List<DemoNoteVO> listCurrentDemoNotes();

    /** 其他启用中租户（排除当前站） */
    List<TenantDirectoryItemVO> listOtherTenants();
}
