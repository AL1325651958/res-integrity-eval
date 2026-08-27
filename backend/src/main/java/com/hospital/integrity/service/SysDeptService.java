package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.entity.SysDept;
import com.hospital.integrity.entity.SysUser;
import com.hospital.integrity.mapper.SysDeptMapper;
import com.hospital.integrity.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 科室管理
 */
@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    /** 树形结构 */
    public List<SysDept> tree() {
        List<SysDept> all = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSortOrder));
        return buildTree(all, 0L);
    }

    public void save(SysDept dept) {
        if (dept.getDeptId() == null) {
            dept.setStatus(dept.getStatus() == null ? 1 : dept.getStatus());
            deptMapper.insert(dept);
        } else {
            deptMapper.updateById(dept);
        }
    }

    public void delete(Long id) {
        Long children = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException("存在下级科室，无法删除");
        }
        Long users = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, id)
                .eq(SysUser::getDelFlag, 0));
        if (users != null && users > 0) {
            throw new BusinessException("科室下存在人员，无法删除");
        }
        deptMapper.deleteById(id);
    }

    private List<SysDept> buildTree(List<SysDept> all, Long parentId) {
        List<SysDept> result = new ArrayList<>();
        for (SysDept dept : all) {
            if (parentId.equals(dept.getParentId())) {
                dept.setChildren(buildTree(all, dept.getDeptId()));
                result.add(dept);
            }
        }
        return result;
    }
}
