package com.liuhecai.controller;

import com.liuhecai.common.result.Result;
import com.liuhecai.service.UploadService;
import com.liuhecai.vo.UploadResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/agent/uploads")
@RequiredArgsConstructor
public class AgentUploadController {

    private final UploadService uploadService;

    @PostMapping
    public Result<UploadResultVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(uploadService.uploadImage(file));
    }
}
