package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件表
 */
@Data
@TableName("research_attachment")
public class ResearchAttachment {

    /** 附件ID */
    @TableId(type = IdType.AUTO)
    private Long fileId;

    /** 业务类型：ACH成果/CHECK工单/APPEAL申诉 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 原始文件名 */
    private String fileName;

    /** 存储路径（UUID命名） */
    private String filePath;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 文件扩展名 */
    private String fileType;

    /** 文件MD5（去重） */
    private String md5;

    /** 是否加密存储：1是 0否 */
    private Integer isEncrypted;

    /** 上传人 */
    private Long uploadBy;

    /** 上传时间 */
    private LocalDateTime uploadTime;
}
