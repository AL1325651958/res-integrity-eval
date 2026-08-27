package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知消息表
 */
@Data
@TableName("research_notice")
public class ResearchNotice {

    /** 通知ID */
    @TableId(type = IdType.AUTO)
    private Long noticeId;

    /** 接收人用户ID */
    private Long userId;

    /** 类型：RISK预警 AUDIT审核 APPEAL申诉 EVALUATE评价 SYSTEM系统 */
    private String noticeType;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 关联业务类型 */
    private String bizType;

    /** 关联业务ID */
    private Long bizId;

    /** 是否已读：0未读 1已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
