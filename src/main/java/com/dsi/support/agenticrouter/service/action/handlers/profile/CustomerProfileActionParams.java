package com.dsi.support.agenticrouter.service.action.handlers.profile;

import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CustomerProfileActionParams {

    public static final String KEY_COMPANY_NAME = "company_name";
    public static final String KEY_PHONE_NUMBER = "phone_number";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_CITY = "city";
    public static final String KEY_POSTAL_CODE = "postal_code";
    public static final String KEY_PREFERRED_LANGUAGE_CODE = "preferred_language_code";

    private final Map<String, Object> values;

    public CustomerProfileActionParams(
        Map<String, Object> values
    ) {
        this.values = Objects.requireNonNull(values, "values");
    }

    public Optional<String> companyName() {
        return text(KEY_COMPANY_NAME);
    }

    public Optional<String> phoneNumber() {
        return text(KEY_PHONE_NUMBER);
    }

    public Optional<String> address() {
        return text(KEY_ADDRESS);
    }

    public Optional<String> city() {
        return text(KEY_CITY);
    }

    public Optional<String> postalCode() {
        return text(KEY_POSTAL_CODE);
    }

    public Optional<String> preferredLanguageCode() {
        return text(KEY_PREFERRED_LANGUAGE_CODE).map(value -> value.toUpperCase(Locale.ROOT));
    }

    private Optional<String> text(
        String key
    ) {
        return Optional.ofNullable(values.get(key))
                       .map(Object::toString)
                       .map(StringNormalizationUtils::trimToNull)
                       .filter(StringUtils::isNotBlank);
    }
}
