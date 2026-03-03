package com.dsi.support.agenticrouter.service.auth;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.*;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.CustomerProfileRepository;
import com.dsi.support.agenticrouter.repository.UserSettingsRepository;
import com.dsi.support.agenticrouter.service.common.UserMeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProfileQueryService {

    private final AuthCurrentUserService authCurrentUserService;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserMeMapper userMeMapper;

    @Transactional(readOnly = true)
    public ApiDtos.ProfileResponse getMyProfile() {
        AppUser appUser = authCurrentUserService.requireCurrentUser();

        CustomerProfile customerProfile = resolveProfileForContext(
            appUser
        );

        return toProfileResponse(
            appUser,
            customerProfile
        );
    }

    @Transactional(readOnly = true)
    public ApiDtos.UserSettingsResponse getMySettings() {
        AppUser appUser = authCurrentUserService.requireCurrentUser();

        UserSettings settings = loadOrCreateUserSettings(appUser);

        return toSettingsResponse(settings);
    }

    private CustomerProfile resolveProfileForContext(
        AppUser appUser
    ) {
        if (!Objects.equals(appUser.getRole(), UserRole.CUSTOMER)) {
            return null;
        }

        return customerProfileRepository.findByUserId(appUser.getId())
                                        .orElseThrow(
                                            DataNotFoundException.supplier(
                                                CustomerProfile.class,
                                                appUser.getId()
                                            )
                                        );
    }

    private UserSettings loadOrCreateUserSettings(AppUser appUser) {
        return userSettingsRepository.findByUserId(appUser.getId())
                                     .orElseGet(() -> UserSettings.builder()
                                                                  .user(appUser)
                                                                  .build());
    }

    private ApiDtos.ProfileResponse toProfileResponse(
        AppUser appUser,
        CustomerProfile customerProfile
    ) {
        ApiDtos.UserMe userMe = userMeMapper.toUserMe(
            appUser
        );

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
