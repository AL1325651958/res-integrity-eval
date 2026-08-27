package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.entity.ResearchAttachment;
import com.hospital.integrity.mapper.ResearchAttachmentMapper;
import com.hospital.integrity.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 附件服务：本地磁盘存储 + 元数据入库 + 鉴权下载
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final ResearchAttachmentMapper attachmentMapper;

    @Value("${integrity.file.path}")
    private String basePath;

    private static final long MAX_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT =
            Set.of("pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx", "zip");

    public ResearchAttachment upload(MultipartFile file, String bizType, Long bizId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("文件大小不能超过20MB");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = ext(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("不支持的文件类型：" + ext);
        }
        try {
            String dir = basePath + "/" + safe(bizType) + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            Path dirPath = Paths.get(dir);
            Files.createDirectories(dirPath);
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String fileName = uuid + "." + ext;
            Path target = dirPath.resolve(fileName);
            file.transferTo(target);

            ResearchAttachment att = new ResearchAttachment();
            att.setBizType(bizType);
            att.setBizId(bizId == null ? 0L : bizId);
            att.setFileName(original);
            att.setFilePath(target.toString());
            att.setFileSize(file.getSize());
            att.setFileType(ext);
            att.setMd5(md5(file));
            att.setIsEncrypted(0);
            att.setUploadBy(SecurityUtils.currentUserId());
            attachmentMapper.insert(att);
            return att;
        } catch (IOException e) {
            log.error("附件上传失败", e);
            throw new BusinessException("附件上传失败");
        }
    }

    public void download(Long id, HttpServletResponse response) {
        ResearchAttachment att = attachmentMapper.selectById(id);
        if (att == null) {
            throw new BusinessException("附件不存在");
        }
        Path path = Paths.get(att.getFilePath());
        if (!Files.exists(path)) {
            throw new BusinessException("附件文件已丢失");
        }
        response.setContentType("application/octet-stream");
        String encoded = UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "." + att.getFileType();
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encoded + "\"");
        try (InputStream in = Files.newInputStream(path); OutputStream out = response.getOutputStream()) {
            in.transferTo(out);
            out.flush();
        } catch (IOException e) {
            log.error("附件下载失败", e);
        }
    }

    public void delete(Long id) {
        ResearchAttachment att = attachmentMapper.selectById(id);
        if (att == null) {
            return;
        }
        attachmentMapper.deleteById(id);
        try {
            Files.deleteIfExists(Paths.get(att.getFilePath()));
        } catch (IOException e) {
            log.warn("删除附件文件失败: {}", att.getFilePath());
        }
    }

    private String ext(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase();
    }

    private String safe(String s) {
        return s == null || s.isBlank() ? "other" : s.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private String md5(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                digest.update(buf, 0, n);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
