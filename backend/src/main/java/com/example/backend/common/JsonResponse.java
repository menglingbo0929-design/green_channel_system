package com.example.backend.common;

import java.io.Serializable;

/**
 * 统一响应格式 —— 对齐 docs/api-document.md 规范
 *
 * 成功：{"code": 200, "message": "success", "data": {…}}
 * 失败：{"code": 400, "message": "用户名或密码错误", "data": null}
 */
public class JsonResponse<R> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private R data;

    // ========== 工厂方法 ==========

    public static <R> JsonResponse<R> success(R data) {
        JsonResponse<R> r = new JsonResponse<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <R> JsonResponse<R> success(R data, String message) {
        JsonResponse<R> r = new JsonResponse<>();
        r.code = 200;
        r.message = message;
        r.data = data;
        return r;
    }

    public static <R> JsonResponse<R> successMessage(String message) {
        JsonResponse<R> r = new JsonResponse<>();
        r.code = 200;
        r.message = message;
        return r;
    }

    public static <R> JsonResponse<R> failure(String message) {
        JsonResponse<R> r = new JsonResponse<>();
        r.code = 400;
        r.message = message;
        return r;
    }

    // ========== Getter / Setter ==========

    public int getCode() { return code; }
    public JsonResponse<R> setCode(int code) { this.code = code; return this; }

    public String getMessage() { return message; }
    public JsonResponse<R> setMessage(String message) { this.message = message; return this; }

    public R getData() { return data; }
    public JsonResponse<R> setData(R data) { this.data = data; return this; }
}
