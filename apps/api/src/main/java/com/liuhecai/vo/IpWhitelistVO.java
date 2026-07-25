package com.liuhecai.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class IpWhitelistVO {
    private boolean enabled;
    private String currentIp;
    private List<IpWhitelistEntryVO> entries = new ArrayList<>();
}
