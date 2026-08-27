package com.hospital.integrity.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页响应
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private List<T> list;

    public static <T> PageResult<T> of(long total, List<T> list) {
        return new PageResult<>(total, list);
    }
}
