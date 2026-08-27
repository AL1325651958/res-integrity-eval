package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.entity.SysMenu;
import com.hospital.integrity.entity.SysRole;
import com.hospital.integrity.entity.SysRoleMenu;
import com.hospital.integrity.mapper.SysMenuMapper;
import com.hospital.integrity.mapper.SysRoleMapper;
import com.hospital.integrity.mapper.SysRoleMenuMapper;
import com.hospital.integrity.security.LoginUser;
import com.hospital.integrity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单管理：当前用户菜单树（动态路由）+ 管理员全量管理
 */
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /** 当前用户可见菜单树（前端动态路由用） */
    public List<SysMenu> treeForUser() {
        LoginUser user = SecurityUtils.currentUser();
        List<SysMenu> menus;
        if (user.getRoleKeys().contains("admin")) {
            menus = allMenus();
        } else {
            Set<String> roleKeys = user.getRoleKeys();
            List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                    .in(SysRole::getRoleKey, roleKeys));
            if (roles.isEmpty()) {
                return List.of();
            }
            Set<Long> roleIds = roles.stream().map(SysRole::getRoleId).collect(Collectors.toSet());
            List<SysRoleMenu> rmList = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                    .in(SysRoleMenu::getRoleId, roleIds));
            if (rmList.isEmpty()) {
                return List.of();
            }
            Set<Long> menuIds = rmList.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
            menus = menuMapper.selectBatchIds(menuIds);
        }
        List<SysMenu> sorted = menus.stream().sorted((a, b) -> {
            int cmp = Integer.compare(a.getSortOrder() == null ? 0 : a.getSortOrder(),
                    b.getSortOrder() == null ? 0 : b.getSortOrder());
            return cmp != 0 ? cmp : Long.compare(a.getMenuId(), b.getMenuId());
        }).toList();
        return buildTree(sorted, 0L);
    }

    public List<SysMenu> treeAll() {
        return buildTree(allMenus(), 0L);
    }

    public void save(SysMenu menu) {
        if (menu.getMenuId() == null) {
            menu.setStatus(menu.getStatus() == null ? 1 : menu.getStatus());
            menuMapper.insert(menu);
        } else {
            menuMapper.updateById(menu);
        }
    }

    public void delete(Long id) {
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException("存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
    }

    private List<SysMenu> allMenus() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder));
    }

    private List<SysMenu> buildTree(List<SysMenu> all, Long parentId) {
        List<SysMenu> result = new ArrayList<>();
        for (SysMenu menu : all) {
            if (parentId.equals(menu.getParentId())) {
                menu.setChildren(buildTree(all, menu.getMenuId()));
                result.add(menu);
            }
        }
        return result;
    }
}
