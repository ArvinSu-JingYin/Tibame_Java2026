package com.tibame.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private boolean success;
    private String message;
    private T data;
    @Builder.Default
    private long timestamp = Instant.now().toEpochMilli();

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .success(true)
                .message("操作成功")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
