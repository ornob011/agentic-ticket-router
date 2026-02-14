package com.dsi.support.agenticrouter.exception;

import java.util.function.Supplier;

public class DataNotFoundException extends ApplicationException {

    public DataNotFoundException(
        String message
    ) {
        super(
            ErrorCode.DATA_NOT_FOUND,
            message
        );
    }

    public DataNotFoundException(
        String message,
        Throwable cause
    ) {
        super(
            ErrorCode.DATA_NOT_FOUND,
            message,
            cause
        );
    }

    public <T> DataNotFoundException(
        Class<T> tClass,
        Object id
    ) {
        super(
            ErrorCode.DATA_NOT_FOUND,
            String.format("Could not find %s with id: %s", tClass.getSimpleName(), id)
        );
    }

    public static <T> Supplier<DataNotFoundException> supplier(
        Class<T> tClass,
        Object id
    ) {
        return () -> new DataNotFoundException(
            tClass,
            id
        );
    }
}
