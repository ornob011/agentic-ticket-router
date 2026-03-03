package com.dsi.support.agenticrouter.service.routing.contract;

import com.dsi.support.agenticrouter.enums.RoutingActionParameterKey;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class RoutingActionParameterReader {

    public long requirePositiveLong(
        Map<String, ?> actionParameters,
        RoutingActionParameterKey parameterKey,
        String missingMessage,
        String numericMessage,
        String positiveMessage
    ) {
        String rawValue = text(
            actionParameters,
            parameterKey
        );
        if (StringUtils.isBlank(rawValue)) {
            throw new IllegalStateException(missingMessage);
        }

        if (!StringUtils.isNumeric(rawValue)) {
            throw new IllegalStateException(numericMessage);
        }

        long parsedValue = Long.parseLong(rawValue);
        if (parsedValue <= 0L) {
            throw new IllegalStateException(positiveMessage);
        }

        return parsedValue;
    }

    public String text(
        Map<String, ?> actionParameters,
        RoutingActionParameterKey parameterKey
    ) {
        if (Objects.isNull(actionParameters)) {
            return null;
        }

        Object value = actionParameters.get(parameterKey.getKey());
        return StringNormalizationUtils.trimToNull(
            Objects.toString(value, null)
        );
    }
}
