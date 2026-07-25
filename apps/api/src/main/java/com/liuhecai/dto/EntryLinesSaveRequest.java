package com.liuhecai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EntryLinesSaveRequest {
    @Valid
    @NotNull
    private List<EntryLineItemRequest> lines = new ArrayList<>();
}
