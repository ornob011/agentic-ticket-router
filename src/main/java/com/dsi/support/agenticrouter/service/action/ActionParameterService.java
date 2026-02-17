package com.dsi.support.agenticrouter.service.action;

import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ActionParameterService {

    public Map<String, ?> requireParameters(
        Map<String, ?> actionParameters,
        String objectName
    ) throws BindException {
        if (Objects.nonNull(actionParameters)) {
            return actionParameters;
        }

        throw BindValidation.fieldError(
            objectName,
            "actionParameters",
            "actionParameters are required"
        );
    }

    public Long requireNumericLong(
        Map<String, ?> actionParameters,
        String objectName,
        String fieldName
    ) throws BindException {
        String rawValue = StringNormalizationUtils.trimToNull(
            Objects.toString(actionParameters.get(fieldName), null)
        );

        if (Objects.isNull(rawValue)) {
            throw BindValidation.fieldError(
                objectName,
                fieldName,
                fieldName + " is required"
            );
        }

        if (!StringUtils.isNumeric(rawValue)) {
            throw BindValidation.fieldError(
                objectName,
                fieldName,
                fieldName + " must be numeric"
            );
        }

        long value = Long.parseLong(rawValue);
        if (value <= 0L) {
            throw BindValidation.fieldError(
                objectName,
                fieldName,
                fieldName + " must be positive"
            );
        }

        return value;
    }

    public Map<String, String> stringValuesExcluding(
        Map<String, ?> actionParameters,
        Set<String> excludedKeys
    ) {
        Map<String, String> variables = new HashMap<>();

        actionParameters.forEach((key, value) -> {
            if (excludedKeys.contains(key)) {
                return;
            }

            String normalizedValue = StringNormalizationUtils.trimToNull(
                Objects.toString(value, null)
            );
            if (Objects.isNull(normalizedValue)) {
                return;
            }

            variables.put(
                key,
                normalizedValue
            );
        });

        return variables;
    }
}
