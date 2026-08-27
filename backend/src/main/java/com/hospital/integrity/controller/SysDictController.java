package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.entity.SysDictData;
import com.hospital.integrity.entity.SysDictType;
import com.hospital.integrity.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典接口
 */
@RestController
@RequestMapping("/v1/system/dict")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SysDictController {

    private final SysDictService dictService;

    @GetMapping("/type/list")
    public Result<List<SysDictType>> typeList() {
        return Result.ok(dictService.typeList());
    }

    @GetMapping("/data/{dictType}")
    public Result<List<SysDictData>> data(@PathVariable String dictType) {
        return Result.ok(dictService.dataByType(dictType));
    }

    @PostMapping("/type")
    @Log(module = "系统", operation = "新增字典类型")
    public Result<Void> saveType(@RequestBody SysDictType type) {
        dictService.saveType(type);
        return Result.ok();
    }

    @PutMapping("/type")
    @Log(module = "系统", operation = "编辑字典类型")
    public Result<Void> updateType(@RequestBody SysDictType type) {
        dictService.saveType(type);
        return Result.ok();
    }

    @DeleteMapping("/type/{id}")
    @Log(module = "系统", operation = "删除字典类型")
    public Result<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.ok();
    }

    @PostMapping("/data")
    @Log(module = "系统", operation = "新增字典数据")
    public Result<Void> saveData(@RequestBody SysDictData data) {
        dictService.saveData(data);
        return Result.ok();
    }

    @PutMapping("/data")
    @Log(module = "系统", operation = "编辑字典数据")
    public Result<Void> updateData(@RequestBody SysDictData data) {
        dictService.saveData(data);
        return Result.ok();
    }

    @DeleteMapping("/data/{id}")
    @Log(module = "系统", operation = "删除字典数据")
    public Result<Void> deleteData(@PathVariable Long id) {
        dictService.deleteData(id);
        return Result.ok();
    }
}
