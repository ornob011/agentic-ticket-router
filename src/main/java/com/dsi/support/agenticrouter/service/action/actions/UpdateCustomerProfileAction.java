package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.CustomerProfile;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.repository.CustomerProfileRepository;
import com.dsi.support.agenticrouter.repository.LanguageRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.NotificationService;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
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
    private final TicketMessageRepository ticketMessageRepository;

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
        log.info(
            "UpdateCustomerProfileAction({}) SupportTicket(id:{},status:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus()
        );

        ActionParams actionParams = new ActionParams(
            Objects.requireNonNull(
                routerResponse.getActionParameters(),
                "Action parameters are required"
            )
        );

        CustomerProfile customerProfile = supportTicket.getCustomer()
                                                       .getCustomerProfile();

        ProfileSnapshot beforeSnapshot = ProfileSnapshot.from(
            customerProfile
        );

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

        boolean hasChanges = changeLog.hasChanges();

        String noChangeDetails = renderNoChangeDetails(
            actionParams,
            beforeSnapshot,
            customerProfile
        );

        Map<Boolean, String> systemMessages = Map.of(
            Boolean.TRUE,
            "Your profile has been updated: " + changeLog.render(),
            Boolean.FALSE,
            "No profile changes were necessary.\n" + noChangeDetails
        );

        Map<Boolean, String> auditMessages = Map.of(
            Boolean.TRUE,
            "Customer profile updated: " + changeLog.render(),
            Boolean.FALSE,
            "Customer profile update requested, but no changes were necessary."
        );

        Map<Boolean, String> notificationTitles = Map.of(
            Boolean.TRUE,
            "Profile Updated",
            Boolean.FALSE,
            "No Profile Changes"
        );

        Map<Boolean, String> notificationBodies = Map.of(
            Boolean.TRUE,
            "Your profile has been updated based on your ticket information.",
            Boolean.FALSE,
            "We reviewed your ticket information, but your profile already matched it. No changes were made."
        );

        Optional.of(hasChanges)
                .filter(Boolean::booleanValue)
                .ifPresent(
                    ignored -> customerProfileRepository.save(customerProfile)
                );

        TicketMessage systemMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .messageKind(MessageKind.SYSTEM_MESSAGE)
                                                   .content(systemMessages.get(hasChanges))
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(systemMessage);

        supportTicket.setStatus(TicketStatus.RESOLVED);
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            auditMessages.get(hasChanges),
            null
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            notificationTitles.get(hasChanges),
            notificationBodies.get(hasChanges),
            supportTicket.getId()
        );

        log.info(
            "UpdateCustomerProfileAction({}) SupportTicket(id:{},status:{}) Outcome(profileChanged:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            hasChanges
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

    private String renderNoChangeDetails(
        ActionParams actionParams,
        ProfileSnapshot beforeSnapshot,
        CustomerProfile currentProfile
    ) {
        Map<String, Comparison> comparisons = new LinkedHashMap<>();

        actionParams.companyName()
                    .ifPresent(requested -> comparisons.put(
                        KEY_COMPANY_NAME,
                        new Comparison(
                            requested,
                            beforeSnapshot.companyName(),
                            currentProfile.getCompanyName()
                        )
                    ));

        actionParams.phoneNumber()
                    .ifPresent(requested -> comparisons.put(
                        KEY_PHONE_NUMBER,
                        new Comparison(
                            requested,
                            beforeSnapshot.phoneNumber(),
                            currentProfile.getPhoneNumber()
                        )
                    ));

        actionParams.address()
                    .ifPresent(requested -> comparisons.put(
                        KEY_ADDRESS,
                        new Comparison(
                            requested,
                            beforeSnapshot.address(),
                            currentProfile.getAddress()
                        )
                    ));

        actionParams.city()
                    .ifPresent(requested -> comparisons.put(
                        KEY_CITY,
                        new Comparison(
                            requested,
                            beforeSnapshot.city(),
                            currentProfile.getCity()
                        )
                    ));

        actionParams.postalCode()
                    .ifPresent(requested -> comparisons.put(
                        KEY_POSTAL_CODE,
                        new Comparison(
                            requested,
                            beforeSnapshot.postalCode(),
                            currentProfile.getPostalCode()
                        )
                    ));

        actionParams.preferredLanguageCode()
                    .ifPresent(requested -> comparisons.put(
                        KEY_PREFERRED_LANGUAGE_CODE,
                        new Comparison(
                            requested,
                            beforeSnapshot.preferredLanguageCode(),
                            currentProfile.getPreferredLanguage().getCode()
                        )
                    ));

        StringBuilder stringBuilder = new StringBuilder();

        Optional.of(comparisons.isEmpty())
                .filter(Boolean::booleanValue)
                .ifPresent(
                    ignored -> stringBuilder.append("No updatable fields were provided in the ticket payload.")
                );

        Optional.of(comparisons.isEmpty())
                .filter(isEmpty -> !isEmpty)
                .ifPresent(
                    ignored -> {
                        stringBuilder.append("Requested vs current values:\n");
                        comparisons.forEach((field, comparison) -> stringBuilder.append("- ")
                                                                                .append(field)
                                                                                .append(": requested='")
                                                                                .append(safe(comparison.requested()))
                                                                                .append("' | current='")
                                                                                .append(safe(comparison.current()))
                                                                                .append("'\n"));
                    }
                );

        return stringBuilder.toString();
    }

    private String safe(
        Object value
    ) {
        return Objects.toString(value, StringUtils.EMPTY);
    }

    private record ActionParams(
        Map<String, Object> values
    ) {

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

    private record ProfileSnapshot(
        String companyName,
        String phoneNumber,
        String address,
        String city,
        String postalCode,
        String preferredLanguageCode
    ) {

        public static ProfileSnapshot from(
            CustomerProfile profile
        ) {
            return new ProfileSnapshot(
                profile.getCompanyName(),
                profile.getPhoneNumber(),
                profile.getAddress(),
                profile.getCity(),
                profile.getPostalCode(),
                profile.getPreferredLanguage().getCode()
            );
        }
    }

    private record Comparison(
        Object requested,
        Object before,
        Object current
    ) {

    }
}
