package com.dsi.support.agenticrouter.exception;

import lombok.NoArgsConstructor;

import java.util.function.Supplier;

@NoArgsConstructor
public class DataNotFoundException extends RuntimeException {

    public DataNotFoundException(
        String message
    ) {
        super(message);
    }

    public DataNotFoundException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }

    public <T> DataNotFoundException(
        Class<T> tClass,
        Object id
    ) {
        super(
            String.format(
                "Could not find %s with id: %s",
                tClass.getSimpleName(),
                id
            )
        );
    }

    public static <T> Supplier<DataNotFoundException> supplier(Class<T> tClass, Object id) {
        return () -> new DataNotFoundException(
            tClass,
            id
        );
    }
}
