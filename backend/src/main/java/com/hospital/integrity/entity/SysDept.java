package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 科室表
 */
@Data
@TableName("sys_dept")
public class SysDept {

    /** 科室ID */
    @TableId(type = IdType.AUTO)
    private Long deptId;

    /** 上级科室ID，0为顶级 */
    private Long parentId;

    /** 科室名称 */
    private String deptName;

    /** 科室编码 */
    private String deptCode;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 子科室（非表字段） */
    @TableField(exist = false)
    private List<SysDept> children;
}
