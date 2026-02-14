package com.dsi.support.agenticrouter.service.auth;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.*;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.*;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;

import java.util.Objects;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AppUserRepository appUserRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CountryRepository countryRepository;
    private final CustomerTierRepository customerTierRepository;
    private final LanguageRepository languageRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public ApiDtos.ProfileResponse getMyProfile() {
        AppUser appUser = getCurrentUser();
        CustomerProfile customerProfile = loadCustomerProfile(appUser);
        return toProfileResponse(
            appUser,
            customerProfile
        );
    }

    @Transactional
    public ApiDtos.ProfileResponse updateMyProfile(
        ApiDtos.ProfileUpdateRequest request
    ) throws BindException {
        AppUser appUser = getCurrentUser();

        String normalizedEmail = StringUtils.lowerCase(
            StringUtils.trimToEmpty(request.email())
        );
        if (appUserRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, appUser.getId())) {
            BeanPropertyBindingResult errors = new BeanPropertyBindingResult(
                request,
                "profileUpdateRequest"
            );

            errors.rejectValue(
                "email",
                "Duplicate",
                "Email is already in use."
            );

            throw new BindException(errors);
        }

        appUser.setEmail(normalizedEmail);
        appUser.setFullName(StringUtils.trimToEmpty(request.fullName()));

        CustomerProfile customerProfile = loadCustomerProfile(appUser);
        if (Objects.equals(appUser.getRole(), UserRole.CUSTOMER)) {
            customerProfile = upsertCustomerProfile(
                appUser,
                customerProfile,
                request
            );
        }

        appUserRepository.save(appUser);

        return toProfileResponse(
            appUser,
            customerProfile
        );
    }

    private AppUser getCurrentUser() {
        Long currentUserId = Utils.getLoggedInUserId();
        return appUserRepository.findById(currentUserId)
                                .orElseThrow(
                                    DataNotFoundException.supplier(
                                        AppUser.class,
                                        currentUserId
                                    )
                                );
    }

    private CustomerProfile loadCustomerProfile(
        AppUser appUser
    ) {
        return customerProfileRepository.findByUserId(appUser.getId()).orElse(null);
    }

    private CustomerProfile upsertCustomerProfile(
        AppUser appUser,
        CustomerProfile existingProfile,
        ApiDtos.ProfileUpdateRequest request
    ) throws BindException {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(
            request,
            "profileUpdateRequest"
        );

        String normalizedCompanyName = requiredTrimmedValue(
            request.companyName(),
            "companyName",
            "Company name is required.",
            errors
        );
        String normalizedPhoneNumber = requiredTrimmedValue(
            request.phoneNumber(),
            "phoneNumber",
            "Phone number is required.",
            errors
        );
        String normalizedAddress = requiredTrimmedValue(
            request.address(),
            "address",
            "Address is required.",
            errors
        );
        String normalizedCity = requiredTrimmedValue(
            request.city(),
            "city",
            "City is required.",
            errors
        );
        String normalizedCountryIso2 = requiredUppercaseValue(
            request.countryIso2(),
            "countryIso2",
            "Country is required.",
            errors
        );
        String normalizedTierCode = requiredUppercaseValue(
            request.customerTierCode(),
            "customerTierCode",
            "Tier is required.",
            errors
        );
        String normalizedLanguageCode = requiredTrimmedValue(
            request.preferredLanguageCode(),
            "preferredLanguageCode",
            "Preferred language is required.",
            errors
        );

        Country country = null;
        if (StringUtils.isNotBlank(normalizedCountryIso2)) {
            country = countryRepository.findByIso2(normalizedCountryIso2).orElse(null);
            if (Objects.isNull(country)) {
                errors.rejectValue(
                    "countryIso2",
                    "Invalid",
                    "Country is invalid."
                );
            }
        }

        CustomerTier customerTier = null;
        if (StringUtils.isNotBlank(normalizedTierCode)) {
            customerTier = customerTierRepository.findByCode(normalizedTierCode).orElse(null);
            if (Objects.isNull(customerTier)) {
                errors.rejectValue(
                    "customerTierCode",
                    "Invalid",
                    "Tier is invalid."
                );
            }
        }

        Language language = null;
        if (StringUtils.isNotBlank(normalizedLanguageCode)) {
            language = languageRepository.findByCode(normalizedLanguageCode).orElse(null);
            if (Objects.isNull(language)) {
                errors.rejectValue(
                    "preferredLanguageCode",
                    "Invalid",
                    "Preferred language is invalid."
                );
            }
        }

        if (errors.hasErrors()) {
            throw new BindException(errors);
        }

        CustomerProfile customerProfile = Objects.nonNull(existingProfile)
            ? existingProfile
            : CustomerProfile.builder()
                             .user(appUser)
                             .notificationsEnabled(true)
                             .build();
        customerProfile.setCompanyName(normalizedCompanyName);
        customerProfile.setPhoneNumber(normalizedPhoneNumber);
        customerProfile.setAddress(normalizedAddress);
        customerProfile.setCity(normalizedCity);
        customerProfile.setCountry(country);
        customerProfile.setCustomerTier(customerTier);
        customerProfile.setPreferredLanguage(language);
        appUser.setCustomerProfile(customerProfile);

        return customerProfile;
    }

    private String requiredTrimmedValue(
        String value,
        String fieldName,
        String errorMessage,
        BeanPropertyBindingResult errors
    ) {
        String normalizedValue = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(normalizedValue)) {
            errors.rejectValue(
                fieldName,
                "NotBlank",
                errorMessage
            );
            return null;
        }

        return normalizedValue;
    }

    private String requiredUppercaseValue(
        String value,
        String fieldName,
        String errorMessage,
        BeanPropertyBindingResult errors
    ) {
        String normalized = requiredTrimmedValue(
            value,
            fieldName,
            errorMessage,
            errors
        );

        if (StringUtils.isBlank(normalized)) {
            return null;
        }

        return StringUtils.upperCase(normalized);
    }

    private ApiDtos.ProfileResponse toProfileResponse(
        AppUser appUser,
        CustomerProfile customerProfile
    ) {
        ApiDtos.UserMe userMe = ApiDtos.UserMe.builder()
                                              .id(appUser.getId())
                                              .username(appUser.getUsername())
                                              .email(appUser.getEmail())
                                              .fullName(appUser.getFullName())
                                              .role(appUser.getRole())
                                              .roleLabel(EnumDisplayNameResolver.resolve(
                                                  appUser.getRole()
                                              ))
                                              .build();

        if (!Objects.equals(appUser.getRole(), UserRole.CUSTOMER) || Objects.isNull(customerProfile)) {
            ApiDtos.StaffProfileData staffProfileData = ApiDtos.StaffProfileData.builder()
                                                                                .active(appUser.isActive())
                                                                                .emailVerified(appUser.isEmailVerified())
                                                                                .lastLoginAt(appUser.getLastLoginAt())
                                                                                .lastLoginIp(appUser.getLastLoginIp())
                                                                                .build();
            return ApiDtos.ProfileResponse.builder()
                                          .user(userMe)
                                          .profileContext("STAFF")
                                          .staffProfile(staffProfileData)
                                          .accountCreatedAt(appUser.getCreatedAt())
                                          .accountActive(appUser.isActive())
                                          .build();
        }

        Country country = customerProfile.getCountry();
        CustomerTier customerTier = customerProfile.getCustomerTier();
        Language language = customerProfile.getPreferredLanguage();
        ApiDtos.CustomerProfileData customerProfileData = ApiDtos.CustomerProfileData.builder()
                                                                                     .companyName(customerProfile.getCompanyName())
                                                                                     .phoneNumber(customerProfile.getPhoneNumber())
                                                                                     .address(customerProfile.getAddress())
                                                                                     .city(customerProfile.getCity())
                                                                                     .countryIso2(Objects.isNull(country) ? null : country.getIso2())
                                                                                     .countryName(Objects.isNull(country) ? null : country.getName())
                                                                                     .customerTierCode(
                                                                                         Objects.isNull(customerTier)
                                                                                             ? null
                                                                                             : customerTier.getCode()
                                                                                     )
                                                                                     .customerTierName(
                                                                                         Objects.isNull(customerTier)
                                                                                             ? null
                                                                                             : customerTier.getDisplayName()
                                                                                     )
                                                                                     .preferredLanguageCode(
                                                                                         Objects.isNull(language)
                                                                                             ? null
                                                                                             : language.getCode()
                                                                                     )
                                                                                     .preferredLanguageName(
                                                                                         Objects.isNull(language)
                                                                                             ? null
                                                                                             : language.getName()
                                                                                     )
                                                                                     .notificationsEnabled(
                                                                                         customerProfile.isNotificationsEnabled()
                                                                                     )
                                                                                     .build();

        return ApiDtos.ProfileResponse.builder()
                                      .user(userMe)
                                      .profileContext("CUSTOMER")
                                      .customerProfile(customerProfileData)
                                      .accountCreatedAt(appUser.getCreatedAt())
                                      .accountActive(appUser.isActive())
                                      .build();
    }

    @Transactional(readOnly = true)
    public ApiDtos.UserSettingsResponse getMySettings() {
        AppUser appUser = getCurrentUser();
        UserSettings settings = loadOrCreateUserSettings(appUser);
        return toSettingsResponse(settings);
    }

    @Transactional
    public ApiDtos.UserSettingsResponse updateMySettings(ApiDtos.UserSettingsUpdateRequest request) {
        AppUser appUser = getCurrentUser();
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

        return toSettingsResponse(settings);
    }

    @Transactional
    public void changePassword(
        ApiDtos.ChangePasswordRequest request
    ) throws BindException {
        AppUser appUser = getCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), appUser.getPasswordHash())) {
            BeanPropertyBindingResult errors = new BeanPropertyBindingResult(
                request,
                "changePasswordRequest"
            );

            errors.rejectValue(
                "currentPassword",
                "Invalid",
                "Current password is incorrect."
            );

            throw new BindException(errors);
        }

        if (!Objects.equals(request.newPassword(), request.confirmNewPassword())) {
            BeanPropertyBindingResult errors = new BeanPropertyBindingResult(
                request,
                "changePasswordRequest"
            );

            errors.rejectValue(
                "confirmNewPassword",
                "Mismatch",
                "New passwords do not match."
            );

            throw new BindException(errors);
        }

        appUser.setPasswordHash(
            passwordEncoder.encode(request.newPassword())
        );

        appUserRepository.save(appUser);
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

    private ApiDtos.UserSettingsResponse toSettingsResponse(UserSettings settings) {
        return ApiDtos.UserSettingsResponse.builder()
                                           .defaultLanding(settings.getDefaultLanding().name())
                                           .defaultLandingLabel(settings.getDefaultLanding().getDisplayName())
                                           .sidebarCollapsed(settings.isSidebarCollapsed())
                                           .theme(settings.getTheme().name())
                                           .themeLabel(settings.getTheme().getDisplayName())
                                           .compactMode(settings.isCompactMode())
                                           .emailNotificationsEnabled(settings.isEmailNotificationsEnabled())
                                           .notifyTicketReply(settings.isNotifyTicketReply())
                                           .notifyStatusChange(settings.isNotifyStatusChange())
                                           .notifyEscalation(settings.isNotifyEscalation())
                                           .build();
    }
}
