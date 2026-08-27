package com.hospital.integrity.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.entity.*;
import com.hospital.integrity.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户加载：账号 → 用户 + 角色 + 权限
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDelFlag, 0));
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在");
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setRealName(user.getRealName());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setTitle(user.getTitle());
        loginUser.setEnabled(user.getStatus() == 1);

        // 角色
        List<SysUserRole> urList = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getUserId()));
        if (!urList.isEmpty()) {
            Set<Long> roleIds = urList.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
            List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
            Set<String> roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());
            loginUser.setRoleKeys(roleKeys);

            // 权限（按钮级 perms）
            List<SysRoleMenu> rmList = sysRoleMenuMapper.selectList(
                    new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
            if (!rmList.isEmpty()) {
                Set<Long> menuIds = rmList.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
                List<SysMenu> menus = sysMenuMapper.selectBatchIds(menuIds);
                Set<String> perms = menus.stream()
                        .filter(m -> "F".equals(m.getMenuType()))
                        .map(SysMenu::getPerms)
                        .filter(p -> p != null && !p.isBlank())
                        .collect(Collectors.toSet());
                loginUser.setPerms(perms);
            }
        }
        return loginUser;
    }
}
