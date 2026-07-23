package com.liuhecai.service.impl;

import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.config.UploadProperties;
import com.liuhecai.service.UploadService;
import com.liuhecai.tenant.TenantContext;
import com.liuhecai.vo.UploadResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Map<String, String> EXT_TO_MIME = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp"
    );

    private final UploadProperties uploadProperties;

    @Override
    public UploadResultVO uploadImage(MultipartFile file) {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.DOMAIN_NOT_FOUND);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择图片文件");
        }
        if (file.getSize() > uploadProperties.getMaxBytes()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片不能超过 2MB");
        }

        String original = file.getOriginalFilename();
        String ext = extensionOf(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 jpg/png/gif/webp");
        }

        String contentType = file.getContentType();
        String expectedMime = EXT_TO_MIME.get(ext);
        if (contentType != null && !contentType.equalsIgnoreCase(expectedMime)
                && !(ext.equals("jpg") && "image/jpg".equalsIgnoreCase(contentType))) {
            // 允许浏览器偶发不准确的 content-type，最终以魔数为准
        }

        byte[] header = readHeader(file, 16);
        if (!magicMatches(ext, header)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件内容与扩展名不匹配");
        }

        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path root = Paths.get(uploadProperties.getRoot()).toAbsolutePath().normalize();
        Path dir = root.resolve(String.valueOf(tenantId)).resolve(day);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }

        String url = "/uploads/" + tenantId + "/" + day + "/" + name;
        return new UploadResultVO(url);
    }

    private static String extensionOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] readHeader(MultipartFile file, int n) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(n);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法读取上传文件");
        }
    }

    private static boolean magicMatches(String ext, byte[] h) {
        if (h == null || h.length < 4) {
            return false;
        }
        return switch (ext) {
            case "jpg", "jpeg" -> (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8;
            case "png" -> h[0] == (byte) 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47;
            case "gif" -> h[0] == 'G' && h[1] == 'I' && h[2] == 'F';
            case "webp" -> h.length >= 12
                    && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            default -> false;
        };
    }
}
