package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型表
 */
@Data
@TableName("sys_dict_type")
public class SysDictType {

    /** 字典类型ID */
    @TableId(type = IdType.AUTO)
    private Long dictId;

    /** 字典名称 */
    private String dictName;

    /** 字典类型编码（唯一） */
    private String dictType;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
