package com.liuhecai.service;

import com.liuhecai.dto.EntryLinesSaveRequest;
import com.liuhecai.vo.EntryLineAdminVO;

import java.util.List;

public interface EntryLineAdminService {
    List<EntryLineAdminVO> listByDomainId(Long domainId);

    List<EntryLineAdminVO> replaceLines(Long domainId, EntryLinesSaveRequest request);

    /** ENTRY 域尚无线路时写入默认五条 */
    void seedDefaultsIfEmpty(Long entryDomainId, Long ownerTenantId);
}
