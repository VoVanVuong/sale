package com.example.usertool.common;

public class ApiResponse<T> {

    private int code;
    private String message;
    private T result;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T result) {
        this.code = code;
        this.message = message;
        this.result = result;
    }

    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>(1000, "Success", result);
    }

    public static <T> ApiResponse<T> success(String message, T result) {
        return new ApiResponse<>(1000, message, result);
    }

    public static <T> ApiResponse<T> of(int code, String message, T result) {
        return new ApiResponse<>(code, message, result);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
