package com.liuhecai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CmsMenusSaveRequest {
    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotBlank
        @Size(max = 32)
        private String code;

        @NotBlank
        @Size(max = 64)
        private String title;

        @NotBlank
        @Size(max = 128)
        private String path;

        @NotNull
        private Integer sortNo;

        @NotNull
        private Integer visible;
    }
}
