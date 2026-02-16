package com.dsi.support.agenticrouter.service.action.handlers.profile;

import com.dsi.support.agenticrouter.entity.CustomerProfile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class CustomerProfileNoChangeDetailRenderer {

    public String render(
        CustomerProfileActionParams actionParams,
        CustomerProfile currentProfile
    ) {
        Map<String, Comparison> comparisons = new LinkedHashMap<>();

        actionParams.companyName().ifPresent(requested -> comparisons.put(
            CustomerProfileActionParams.KEY_COMPANY_NAME,
            new Comparison(
                requested,
                currentProfile.getCompanyName()
            )
        ));

        actionParams.phoneNumber().ifPresent(requested -> comparisons.put(
            CustomerProfileActionParams.KEY_PHONE_NUMBER,
            new Comparison(
                requested,
                currentProfile.getPhoneNumber()
            )
        ));

        actionParams.address().ifPresent(requested -> comparisons.put(
            CustomerProfileActionParams.KEY_ADDRESS,
            new Comparison(
                requested,
                currentProfile.getAddress()
            )
        ));

        actionParams.city().ifPresent(requested -> comparisons.put(
            CustomerProfileActionParams.KEY_CITY,
            new Comparison(
                requested,
                currentProfile.getCity()
            )
        ));

        actionParams.postalCode().ifPresent(requested -> comparisons.put(
            CustomerProfileActionParams.KEY_POSTAL_CODE,
            new Comparison(
                requested,
                currentProfile.getPostalCode()
            )
        ));

        actionParams.preferredLanguageCode().ifPresent(requested -> comparisons.put(
            CustomerProfileActionParams.KEY_PREFERRED_LANGUAGE_CODE,
            new Comparison(
                requested,
                currentProfile.getPreferredLanguage().getCode()
            )
        ));

        if (comparisons.isEmpty()) {
            return "No updatable fields were provided in the ticket payload.";
        }

        StringBuilder stringBuilder = new StringBuilder("Requested vs current values:\n");

        comparisons.forEach((field, comparison) -> stringBuilder.append("- ")
                                                                .append(field)
                                                                .append(": requested='")
                                                                .append(safe(comparison.requested()))
                                                                .append("' | current='")
                                                                .append(safe(comparison.current()))
                                                                .append("'\n"));

        return stringBuilder.toString();
    }

    private String safe(
        Object value
    ) {
        return Objects.toString(
            value,
            StringUtils.EMPTY
        );
    }

    private record Comparison(
        Object requested,
        Object current
    ) {
    }
}
