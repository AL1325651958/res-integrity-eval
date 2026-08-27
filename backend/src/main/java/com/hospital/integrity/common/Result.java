package com.hospital.integrity.common;

import lombok.Data;

/**
 * 统一响应体：code=0 成功，非 0 业务错误
 */
@Data
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        return fail(500, msg);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> Result<T> unauthorized() {
        return fail(401, "未登录或登录已过期");
    }

    public static <T> Result<T> forbidden() {
        return fail(403, "无权限操作");
    }
}
