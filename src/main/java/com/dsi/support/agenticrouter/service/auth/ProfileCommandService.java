package com.dsi.support.agenticrouter.service.auth;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.CustomerProfile;
import com.dsi.support.agenticrouter.entity.UserSettings;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.CustomerProfileRepository;
import com.dsi.support.agenticrouter.repository.UserSettingsRepository;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ProfileCommandService {

    private final AppUserRepository appUserRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileQueryService profileQueryService;
    private final ProfileCustomerUpdateResolver profileCustomerUpdateResolver;
    private final AuthCurrentUserService authCurrentUserService;

    @Transactional
    public ApiDtos.ProfileResponse updateMyProfile(
        ApiDtos.ProfileUpdateRequest request
    ) throws BindException {
        AppUser appUser = authCurrentUserService.requireCurrentUser();

        String normalizedEmail = StringNormalizationUtils.lowerTrimmedOrEmpty(request.email());
        if (appUserRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, appUser.getId())) {
            BindingResult errors = BindValidation.bindingResult(
                request,
                "profileUpdateRequest"
            );

            BindValidation.rejectField(
                errors,
                "email",
                "Email is already in use."
            );

            throw BindValidation.exception(errors);
        }

        appUser.setEmail(normalizedEmail);
        appUser.setFullName(StringNormalizationUtils.trimToEmpty(request.fullName()));

        Optional<CustomerProfile> customerProfile = loadCustomerProfile(appUser);
        if (Objects.equals(appUser.getRole(), UserRole.CUSTOMER)) {
            upsertCustomerProfile(
                appUser,
                customerProfile,
                request
            );
        }

        appUserRepository.save(appUser);

        return profileQueryService.getMyProfile();
    }

    @Transactional
    public ApiDtos.UserSettingsResponse updateMySettings(ApiDtos.UserSettingsUpdateRequest request) {
        AppUser appUser = authCurrentUserService.requireCurrentUser();
        UserSettings settings = loadOrCreateUserSettings(appUser);

        applyIfPresent(
            request.defaultLanding(),
            settings::setDefaultLanding
        );

        applyIfPresent(
            request.sidebarCollapsed(),
            settings::setSidebarCollapsed
        );

        applyIfPresent(
            request.theme(),
            settings::setTheme
        );

        applyIfPresent(
            request.compactMode(),
            settings::setCompactMode
        );

        applyIfPresent(
            request.emailNotificationsEnabled(),
            settings::setEmailNotificationsEnabled
        );

        applyIfPresent(
            request.notifyTicketReply(),
            settings::setNotifyTicketReply
        );

        applyIfPresent(
            request.notifyStatusChange(),
            settings::setNotifyStatusChange
        );

        applyIfPresent(
            request.notifyEscalation(),
            settings::setNotifyEscalation
        );

        userSettingsRepository.save(settings);

        return profileQueryService.getMySettings();
    }

    @Transactional
    public void changePassword(
        ApiDtos.ChangePasswordRequest request
    ) throws BindException {
        AppUser appUser = authCurrentUserService.requireCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), appUser.getPasswordHash())) {
            BindingResult errors = BindValidation.bindingResult(
                request,
                "changePasswordRequest"
            );

            BindValidation.rejectField(
                errors,
                "currentPassword",
                "Current password is incorrect."
            );

            throw BindValidation.exception(errors);
        }

        if (!Objects.equals(request.newPassword(), request.confirmNewPassword())) {
            BindingResult errors = BindValidation.bindingResult(
                request,
                "changePasswordRequest"
            );

            BindValidation.rejectField(
                errors,
                "confirmNewPassword",
                "New passwords do not match."
            );

            throw BindValidation.exception(errors);
        }

        appUser.setPasswordHash(
            passwordEncoder.encode(request.newPassword())
        );

        appUserRepository.save(appUser);
    }

    private Optional<CustomerProfile> loadCustomerProfile(
        AppUser appUser
    ) {
        return customerProfileRepository.findByUserId(appUser.getId());
    }

    private void upsertCustomerProfile(
        AppUser appUser,
        Optional<CustomerProfile> existingProfile,
        ApiDtos.ProfileUpdateRequest request
    ) throws BindException {
        ProfileCustomerUpdateResolver.CustomerProfileUpdatePayload payload = profileCustomerUpdateResolver.resolve(
            request
        );

        CustomerProfile customerProfile = existingProfile.orElseGet(() -> CustomerProfile.builder()
                                                                                          .user(appUser)
                                                                                          .notificationsEnabled(true)
                                                                                          .build());
        customerProfile.setCompanyName(payload.companyName());
        customerProfile.setPhoneNumber(payload.phoneNumber());
        customerProfile.setAddress(payload.address());
        customerProfile.setCity(payload.city());
        customerProfile.setCountry(payload.country());
        customerProfile.setCustomerTier(payload.customerTier());
        customerProfile.setPreferredLanguage(payload.preferredLanguage());
        appUser.setCustomerProfile(customerProfile);
    }

    private UserSettings loadOrCreateUserSettings(AppUser appUser) {
        return userSettingsRepository.findByUserId(appUser.getId())
                                     .orElseGet(() -> UserSettings.builder()
                                                                  .user(appUser)
                                                                  .build());
    }

    private <T> void applyIfPresent(
        T value,
        Consumer<T> consumer
    ) {
        if (Objects.nonNull(value)) {
            consumer.accept(value);
        }
    }
}
