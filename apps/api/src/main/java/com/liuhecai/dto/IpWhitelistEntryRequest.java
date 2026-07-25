package com.liuhecai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IpWhitelistEntryRequest {
    @NotBlank
    @Size(max = 64)
    private String cidr;

    @Size(max = 128)
    private String note;
}
