package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.dto.UserDTO;
import com.hospital.integrity.dto.UserVO;
import com.hospital.integrity.entity.*;
import com.hospital.integrity.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理（管理员）
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(int pageNum, int pageSize, String keyword, Long deptId, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDelFlag, 0)
                .eq(status != null, SysUser::getStatus, status)
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(SysUser::getRealName, keyword)
                                .or().like(SysUser::getEmpNo, keyword)
                                .or().like(SysUser::getUsername, keyword))
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<UserVO> vos = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), vos);
    }

    @Transactional
    public void create(UserDTO dto) {
        checkUnique(dto.getEmpNo(), dto.getUsername(), null);
        SysUser user = new SysUser();
        user.setEmpNo(dto.getEmpNo());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword() == null || dto.getPassword().isBlank()
                ? "123456" : dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setDeptId(dto.getDeptId());
        user.setTitle(dto.getTitle());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        sysUserMapper.insert(user);
        bindRoles(user.getUserId(), dto.getRoleIds());
    }

    @Transactional
    public void update(Long id, UserDTO dto) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkUnique(dto.getEmpNo(), dto.getUsername(), id);
        user.setEmpNo(dto.getEmpNo());
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setDeptId(dto.getDeptId());
        user.setTitle(dto.getTitle());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        sysUserMapper.updateById(user);
        if (dto.getRoleIds() != null) {
            sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
            bindRoles(id, dto.getRoleIds());
        }
    }

    @Transactional
    public void delete(Long id) {
        if (com.hospital.integrity.security.SecurityUtils.currentUserId().equals(id)) {
            throw new BusinessException("不能删除当前登录账号");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return;
        }
        user.setDelFlag(1);
        sysUserMapper.updateById(user);
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    public void updateStatus(Long id, Integer status) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    public void resetPwd(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        sysUserMapper.updateById(user);
    }

    private void checkUnique(String empNo, String username, Long excludeId) {
        Long empCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmpNo, empNo)
                .eq(SysUser::getDelFlag, 0)
                .ne(excludeId != null, SysUser::getUserId, excludeId));
        if (empCount != null && empCount > 0) {
            throw new BusinessException("工号已存在");
        }
        Long nameCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDelFlag, 0)
                .ne(excludeId != null, SysUser::getUserId, excludeId));
        if (nameCount != null && nameCount > 0) {
            throw new BusinessException("账号已存在");
        }
    }

    private void bindRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            sysUserRoleMapper.insert(ur);
        }
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setUserId(user.getUserId());
        vo.setEmpNo(user.getEmpNo());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setDeptId(user.getDeptId());
        vo.setTitle(user.getTitle());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        if (user.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(user.getDeptId());
            vo.setDeptName(dept == null ? null : dept.getDeptName());
        }
        List<SysUserRole> urList = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getUserId()));
        if (!urList.isEmpty()) {
            Set<Long> roleIds = urList.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
            List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
            vo.setRoleIds(new ArrayList<>(roleIds));
            vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.joining("、")));
        }
        return vo;
    }
}
