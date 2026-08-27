package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单权限表
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    /** 菜单ID */
    @TableId(type = IdType.AUTO)
    private Long menuId;

    /** 上级菜单ID，0为顶级 */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 类型：M目录 C菜单 F按钮 */
    private String menuType;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识，如 integrity:check:handle */
    private String perms;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：1显示 0隐藏 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 子菜单（非表字段） */
    @TableField(exist = false)
    private List<SysMenu> children;
}
