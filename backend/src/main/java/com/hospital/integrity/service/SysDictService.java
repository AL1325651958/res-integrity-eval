package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.entity.SysDictData;
import com.hospital.integrity.entity.SysDictType;
import com.hospital.integrity.mapper.SysDictDataMapper;
import com.hospital.integrity.mapper.SysDictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典管理
 */
@Service
@RequiredArgsConstructor
public class SysDictService {

    private final SysDictTypeMapper typeMapper;
    private final SysDictDataMapper dataMapper;

    public List<SysDictType> typeList() {
        return typeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getStatus, 1)
                .orderByAsc(SysDictType::getDictId));
    }

    public List<SysDictData> dataByType(String dictType) {
        return dataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSortOrder));
    }

    public void saveType(SysDictType type) {
        if (type.getDictId() == null) {
            Long exists = typeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                    .eq(SysDictType::getDictType, type.getDictType()));
            if (exists != null && exists > 0) {
                throw new com.hospital.integrity.common.BusinessException("字典类型已存在");
            }
            type.setStatus(type.getStatus() == null ? 1 : type.getStatus());
            typeMapper.insert(type);
        } else {
            typeMapper.updateById(type);
        }
    }

    public void deleteType(Long id) {
        SysDictType type = typeMapper.selectById(id);
        if (type != null) {
            dataMapper.delete(new LambdaQueryWrapper<SysDictData>()
                    .eq(SysDictData::getDictType, type.getDictType()));
        }
        typeMapper.deleteById(id);
    }

    public void saveData(SysDictData data) {
        if (data.getDictCode() == null) {
            data.setStatus(data.getStatus() == null ? 1 : data.getStatus());
            dataMapper.insert(data);
        } else {
            dataMapper.updateById(data);
        }
    }

    public void deleteData(Long id) {
        dataMapper.deleteById(id);
    }
}
