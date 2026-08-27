package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.entity.SysRole;
import com.hospital.integrity.entity.SysRoleMenu;
import com.hospital.integrity.entity.SysUserRole;
import com.hospital.integrity.mapper.SysRoleMapper;
import com.hospital.integrity.mapper.SysRoleMenuMapper;
import com.hospital.integrity.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public List<SysRole> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getRoleId));
    }

    public void save(SysRole role) {
        if (role.getRoleId() == null) {
            Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleKey, role.getRoleKey()));
            if (exists != null && exists > 0) {
                throw new BusinessException("角色标识已存在");
            }
            role.setStatus(role.getStatus() == null ? 1 : role.getStatus());
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
    }

    @Transactional
    public void delete(Long id) {
        Long users = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (users != null && users > 0) {
            throw new BusinessException("该角色已分配用户，无法删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    @Transactional
    public void saveRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    public List<Long> roleMenuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }
}
