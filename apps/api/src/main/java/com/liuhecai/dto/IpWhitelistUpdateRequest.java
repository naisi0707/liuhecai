package com.liuhecai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class IpWhitelistUpdateRequest {
    @NotNull
    private Boolean enabled;

    @Valid
    @NotNull
    private List<IpWhitelistEntryRequest> entries = new ArrayList<>();
}
