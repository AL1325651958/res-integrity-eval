package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据表
 */
@Data
@TableName("sys_dict_data")
public class SysDictData {

    /** 字典数据ID */
    @TableId(type = IdType.AUTO)
    private Long dictCode;

    /** 字典类型编码 */
    private String dictType;

    /** 字典标签（中文名） */
    private String dictLabel;

    /** 字典键值 */
    private String dictValue;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
