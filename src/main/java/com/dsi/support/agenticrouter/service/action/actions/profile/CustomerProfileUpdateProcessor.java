package com.dsi.support.agenticrouter.service.action.actions.profile;

import com.dsi.support.agenticrouter.entity.CustomerProfile;
import com.dsi.support.agenticrouter.entity.Language;
import com.dsi.support.agenticrouter.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class CustomerProfileUpdateProcessor {

    private static final String CHANGE_SEPARATOR = ", ";

    private final LanguageRepository languageRepository;
    private final CustomerProfileNoChangeDetailRenderer noChangeDetailRenderer;

    public CustomerProfileUpdateOutcome process(
        CustomerProfile customerProfile,
        Map<String, Object> actionParameters
    ) {
        CustomerProfileActionParams actionParams = new CustomerProfileActionParams(actionParameters);

        List<String> changes = new ArrayList<>();

        updateCompanyName(
            actionParams,
            customerProfile,
            changes
        );
        updatePhoneNumber(
            actionParams,
            customerProfile,
            changes
        );
        updateAddress(
            actionParams,
            customerProfile,
            changes
        );
        updateCity(
            actionParams,
            customerProfile,
            changes
        );
        updatePostalCode(
            actionParams,
            customerProfile,
            changes
        );
        updatePreferredLanguage(
            actionParams,
            customerProfile,
            changes
        );

        boolean hasChanges = !changes.isEmpty();
        String changeSummary = String.join(
            CHANGE_SEPARATOR,
            changes
        );

        return new CustomerProfileUpdateOutcome(
            hasChanges,
            changeSummary,
            noChangeDetailRenderer.render(
                actionParams,
                customerProfile
            )
        );
    }

    private void updateCompanyName(
        CustomerProfileActionParams actionParams,
        CustomerProfile customerProfile,
        List<String> changes
    ) {
        actionParams.companyName().ifPresent(newValue -> applyStringChange(
            CustomerProfileActionParams.KEY_COMPANY_NAME,
            customerProfile.getCompanyName(),
            newValue,
            customerProfile::setCompanyName,
            changes
        ));
    }

    private void updatePhoneNumber(
        CustomerProfileActionParams actionParams,
        CustomerProfile customerProfile,
        List<String> changes
    ) {
        actionParams.phoneNumber().ifPresent(newValue -> applyStringChange(
            CustomerProfileActionParams.KEY_PHONE_NUMBER,
            customerProfile.getPhoneNumber(),
            newValue,
            customerProfile::setPhoneNumber,
            changes
        ));
    }

    private void updateAddress(
        CustomerProfileActionParams actionParams,
        CustomerProfile customerProfile,
        List<String> changes
    ) {
        actionParams.address().ifPresent(newValue -> applyStringChange(
            CustomerProfileActionParams.KEY_ADDRESS,
            customerProfile.getAddress(),
            newValue,
            customerProfile::setAddress,
            changes
        ));
    }

    private void updateCity(
        CustomerProfileActionParams actionParams,
        CustomerProfile customerProfile,
        List<String> changes
    ) {
        actionParams.city().ifPresent(newValue -> applyStringChange(
            CustomerProfileActionParams.KEY_CITY,
            customerProfile.getCity(),
            newValue,
            customerProfile::setCity,
            changes
        ));
    }

    private void updatePostalCode(
        CustomerProfileActionParams actionParams,
        CustomerProfile customerProfile,
        List<String> changes
    ) {
        actionParams.postalCode().ifPresent(newValue -> applyStringChange(
            CustomerProfileActionParams.KEY_POSTAL_CODE,
            customerProfile.getPostalCode(),
            newValue,
            customerProfile::setPostalCode,
            changes
        ));
    }

    private void updatePreferredLanguage(
        CustomerProfileActionParams actionParams,
        CustomerProfile customerProfile,
        List<String> changes
    ) {
        actionParams.preferredLanguageCode().ifPresent(newCode -> {
            Language currentLanguage = customerProfile.getPreferredLanguage();
            String oldCode = Objects.isNull(currentLanguage) ? null : currentLanguage.getCode();

            if (Objects.equals(oldCode, newCode)) {
                return;
            }

            languageRepository.findByCode(newCode)
                              .ifPresent(language -> {
                                  customerProfile.setPreferredLanguage(language);
                                  changes.add(formatChange(
                                      CustomerProfileActionParams.KEY_PREFERRED_LANGUAGE_CODE,
                                      oldCode,
                                      newCode
                                  ));
                              });
        });
    }

    private void applyStringChange(
        String field,
        String oldValue,
        String newValue,
        Consumer<String> setter,
        List<String> changes
    ) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        setter.accept(newValue);
        changes.add(formatChange(
            field,
            oldValue,
            newValue
        ));
    }

    private String formatChange(
        String field,
        String oldValue,
        String newValue
    ) {
        return field + ": " + Objects.toString(oldValue, "") + " -> " + Objects.toString(newValue, "");
    }
}
