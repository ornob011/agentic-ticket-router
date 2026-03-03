package com.dsi.support.agenticrouter.util;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;

public final class BindValidation {

    private BindValidation() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static BindException fieldError(
        String objectName,
        String fieldName,
        String message
    ) {
        MapBindingResult bindingResult = new MapBindingResult(
            new HashMap<>(),
            objectName
        );

        rejectField(
            bindingResult,
            fieldName,
            message
        );

        return new BindException(
            bindingResult
        );
    }

    public static BeanPropertyBindingResult bindingResult(
        Object target,
        String objectName
    ) {
        return new BeanPropertyBindingResult(
            target,
            objectName
        );
    }

    public static void rejectField(
        BindingResult bindingResult,
        String fieldName,
        String message
    ) {
        bindingResult.rejectValue(
            fieldName,
            "validation.error",
            message
        );
    }

    public static BindException exception(
        BindingResult bindingResult
    ) {
        return new BindException(
            bindingResult
        );
    }
}
