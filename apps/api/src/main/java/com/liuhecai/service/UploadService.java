package com.liuhecai.service;

import org.springframework.web.multipart.MultipartFile;
import com.liuhecai.vo.UploadResultVO;

public interface UploadService {
    UploadResultVO uploadImage(MultipartFile file);
}
