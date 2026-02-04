package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.CustomerProfile;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.repository.CustomerProfileRepository;
import com.dsi.support.agenticrouter.repository.LanguageRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.NotificationService;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateCustomerProfileAction implements TicketAction {

    private static final String KEY_COMPANY_NAME = "company_name";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_CITY = "city";
    private static final String KEY_POSTAL_CODE = "postal_code";
    private static final String KEY_PREFERRED_LANGUAGE_CODE = "preferred_language_code";

    private final CustomerProfileRepository customerProfileRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final LanguageRepository languageRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.UPDATE_CUSTOMER_PROFILE.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        ActionParams actionParams = new ActionParams(
            Objects.requireNonNull(
                routerResponse.getActionParameters(),
                "Action parameters are required"
            )
        );

        CustomerProfile customerProfile = supportTicket.getCustomer()
                                                       .getCustomerProfile();

        ProfileChangeLog changeLog = new ProfileChangeLog();

        updateCompanyName(
            actionParams,
            customerProfile,
            changeLog
        );

        updatePhoneNumber(
            actionParams,
            customerProfile,
            changeLog
        );

        updateAddress(
            actionParams,
            customerProfile,
            changeLog
        );

        updateCity(
            actionParams,
            customerProfile,
            changeLog
        );

        updatePostalCode(
            actionParams,
            customerProfile,
            changeLog
        );

        updatePreferredLanguage(
            actionParams,
            customerProfile,
            changeLog
        );

        if (!changeLog.hasChanges()) {
            return;
        }

        customerProfileRepository.save(customerProfile);

        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            "Customer profile updated: " + changeLog.render(),
            null
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            "Profile Updated",
            "Your profile has been updated based on your ticket information.",
            supportTicket.getId()
        );
    }

    private void updateCompanyName(
        ActionParams actionParams,
        CustomerProfile customerProfile,
        ProfileChangeLog changeLog
    ) {
        actionParams.companyName()
                    .ifPresent(newValue -> applyStringChange(
                        KEY_COMPANY_NAME,
                        customerProfile.getCompanyName(),
                        newValue,
                        customerProfile::setCompanyName,
                        changeLog
                    ));
    }

    private void updatePhoneNumber(
        ActionParams actionParams,
        CustomerProfile customerProfile,
        ProfileChangeLog changeLog
    ) {
        actionParams.phoneNumber()
                    .ifPresent(newValue -> applyStringChange(
                        KEY_PHONE_NUMBER,
                        customerProfile.getPhoneNumber(),
                        newValue,
                        customerProfile::setPhoneNumber,
                        changeLog
                    ));
    }

    private void updateAddress(
        ActionParams actionParams,
        CustomerProfile customerProfile,
        ProfileChangeLog changeLog
    ) {
        actionParams.address()
                    .ifPresent(newValue -> applyStringChange(
                        KEY_ADDRESS,
                        customerProfile.getAddress(),
                        newValue,
                        customerProfile::setAddress,
                        changeLog
                    ));
    }

    private void updateCity(
        ActionParams actionParams,
        CustomerProfile customerProfile,
        ProfileChangeLog changeLog
    ) {
        actionParams.city()
                    .ifPresent(newValue -> applyStringChange(
                        KEY_CITY,
                        customerProfile.getCity(),
                        newValue,
                        customerProfile::setCity,
                        changeLog
                    ));
    }

    private void updatePostalCode(
        ActionParams actionParams,
        CustomerProfile customerProfile,
        ProfileChangeLog changeLog
    ) {
        actionParams.postalCode()
                    .ifPresent(newValue -> applyStringChange(
                        KEY_POSTAL_CODE,
                        customerProfile.getPostalCode(),
                        newValue,
                        customerProfile::setPostalCode,
                        changeLog
                    ));
    }

    private void updatePreferredLanguage(
        ActionParams actionParams,
        CustomerProfile customerProfile,
        ProfileChangeLog changeLog
    ) {
        actionParams.preferredLanguageCode()
                    .ifPresent(newCode -> {
                        String oldCode = customerProfile.getPreferredLanguage().getCode();

                        if (Objects.equals(oldCode, newCode)) {
                            return;
                        }

                        languageRepository.findByCode(newCode)
                                          .ifPresent(language -> {
                                              customerProfile.setPreferredLanguage(language);
                                              changeLog.addChange(
                                                  KEY_PREFERRED_LANGUAGE_CODE,
                                                  oldCode,
                                                  newCode
                                              );
                                          });
                    });
    }

    private void applyStringChange(
        String field,
        String oldValue,
        String newValue,
        Consumer<String> setter,
        ProfileChangeLog changeLog
    ) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        setter.accept(newValue);
        changeLog.addChange(field, oldValue, newValue);
    }

    private static final class ActionParams {

        private final Map<String, Object> values;

        private ActionParams(
            Map<String, Object> values
        ) {
            this.values = values;
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
            return text(KEY_PREFERRED_LANGUAGE_CODE)
                .map(String::toUpperCase);
        }

        private Optional<String> text(
            String key
        ) {
            return Optional.ofNullable(values.get(key))
                           .map(Object::toString)
                           .map(StringUtils::trimToNull)
                           .filter(StringUtils::isNotBlank);
        }
    }

    private static final class ProfileChangeLog {

        private static final String CHANGE_SEPARATOR = ", ";

        private final StringBuilder changeDescription = new StringBuilder();
        private boolean changesDetected = false;

        public void addChange(
            String field,
            String oldValue,
            String newValue
        ) {
            changesDetected = true;
            changeDescription.append(field)
                             .append(": ")
                             .append(oldValue)
                             .append(" -> ")
                             .append(newValue)
                             .append(CHANGE_SEPARATOR);
        }

        public boolean hasChanges() {
            return changesDetected;
        }

        public String render() {
            if (!changesDetected) {
                return StringUtils.EMPTY;
            }

            int length = changeDescription.length();
            changeDescription.setLength(length - CHANGE_SEPARATOR.length());
            return changeDescription.toString();
        }
    }
}
