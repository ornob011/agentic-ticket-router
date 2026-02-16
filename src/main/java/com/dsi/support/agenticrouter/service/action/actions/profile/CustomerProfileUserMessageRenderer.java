package com.dsi.support.agenticrouter.service.action.actions.profile;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CustomerProfileUserMessageRenderer {

    private static final Map<String, String> FIELD_LABELS = new LinkedHashMap<>();

    static {
        FIELD_LABELS.put(CustomerProfileActionParams.KEY_COMPANY_NAME, "Company Name");
        FIELD_LABELS.put(CustomerProfileActionParams.KEY_PHONE_NUMBER, "Phone Number");
        FIELD_LABELS.put(CustomerProfileActionParams.KEY_ADDRESS, "Address");
        FIELD_LABELS.put(CustomerProfileActionParams.KEY_CITY, "City");
        FIELD_LABELS.put(CustomerProfileActionParams.KEY_POSTAL_CODE, "Postal Code");
        FIELD_LABELS.put(CustomerProfileActionParams.KEY_PREFERRED_LANGUAGE_CODE, "Preferred Language");
    }

    public String systemMessage(
        CustomerProfileUpdateOutcome updateOutcome
    ) {
        if (updateOutcome.changed()) {
            StringBuilder stringBuilder = new StringBuilder(
                String.format(
                    "Your profile has been updated with %d change%s:",
                    updateOutcome.fieldChanges().size(),
                    updateOutcome.fieldChanges().size() > 1 ? "s" : StringUtils.EMPTY
                )
            );

            updateOutcome.fieldChanges()
                         .forEach(change -> stringBuilder.append("\n- ")
                                                         .append(resolveFieldLabel(change.field()))
                                                         .append(String.format(" changed from '%s' to '%s'.",
                                                             displayValue(change.previousValue()),
                                                             displayValue(change.currentValue())
                                                         )));

            return stringBuilder.toString();
        }

        return "No profile updates were applied because the requested values already match your current profile.";
    }

    public String notificationTitle(
        CustomerProfileUpdateOutcome updateOutcome
    ) {
        if (updateOutcome.changed()) {
            return "Profile Updated";
        }

        return "No Profile Changes";
    }

    public String notificationBody(
        CustomerProfileUpdateOutcome updateOutcome
    ) {
        if (updateOutcome.changed()) {
            return String.format(
                "We updated your profile information based on your request (%d change%s).",
                updateOutcome.fieldChanges().size(),
                updateOutcome.fieldChanges().size() > 1 ? "s" : StringUtils.EMPTY
            );
        }

        return "We reviewed your request, and no profile updates were needed.";
    }

    private String resolveFieldLabel(
        String field
    ) {
        return FIELD_LABELS.getOrDefault(
            field,
            StringUtils.capitalize(
                StringUtils.replaceChars(
                    field,
                    "_",
                    StringUtils.SPACE
                )
            )
        );
    }

    private String displayValue(
        String value
    ) {
        if (StringUtils.isBlank(value)) {
            return "Not set";
        }

        return value;
    }
}
