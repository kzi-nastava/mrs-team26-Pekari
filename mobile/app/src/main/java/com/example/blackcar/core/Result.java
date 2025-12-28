package com.example.blackcar.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class Result<T> {
    public enum Status {
        LOADING,
        SUCCESS,
        ERROR
    }

    @NonNull
    private final Status status;
    @Nullable
    private final T data;
    @Nullable
    private final String errorMessage;

    private Result(@NonNull Status status, @Nullable T data, @Nullable String errorMessage) {
        this.status = status;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @NonNull
    public static <T> Result<T> loading() {
        return new Result<>(Status.LOADING, null, null);
    }

    @NonNull
    public static <T> Result<T> success(@NonNull T data) {
        return new Result<>(Status.SUCCESS, data, null);
    }

    @NonNull
    public static <T> Result<T> error(@NonNull String message) {
        return new Result<>(Status.ERROR, null, message);
    }
}
