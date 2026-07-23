package com.liuhecai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TopicCreateRequest {
    @NotBlank
    @Size(max = 128)
    private String title;

    @NotBlank
    private String lotteryType;

    @NotBlank
    @Size(max = 32)
    private String issueNo;

    @Size(max = 64)
    private String playType;

    @NotNull
    @Min(0)
    private Integer price;

    @NotBlank
    private String content;

    /** 可选：往期成绩等公开预览 HTML */
    private String previewContent;

    /** 可选：创建时直接上架，默认 0 待审 */
    private Integer status;
}
