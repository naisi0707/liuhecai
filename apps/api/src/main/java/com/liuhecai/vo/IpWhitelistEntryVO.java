package com.liuhecai.vo;

import lombok.Data;

@Data
public class IpWhitelistEntryVO {
    private String id;
    private String cidr;
    private String note;
}
