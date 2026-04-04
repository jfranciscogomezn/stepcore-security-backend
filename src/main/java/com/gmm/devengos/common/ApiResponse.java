package com.gmm.devengos.common;

public record ApiResponse<T>(boolean success, T data, String message) {

    public static <T> ApiResponse<T> ok(final T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok(final T data, final String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> error(final String message) {
        return new ApiResponse<>(false, null, message);
    }
}
