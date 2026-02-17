package com.dsi.support.agenticrouter.service.auth;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileQueryService profileQueryService;
    private final ProfileCommandService profileCommandService;

    public ApiDtos.ProfileResponse getMyProfile() {
        return profileQueryService.getMyProfile();
    }

    public ApiDtos.ProfileResponse updateMyProfile(
        ApiDtos.ProfileUpdateRequest request
    ) throws BindException {
        return profileCommandService.updateMyProfile(
            request
        );
    }

    public ApiDtos.UserSettingsResponse getMySettings() {
        return profileQueryService.getMySettings();
    }

    public ApiDtos.UserSettingsResponse updateMySettings(
        ApiDtos.UserSettingsUpdateRequest request
    ) {
        return profileCommandService.updateMySettings(
            request
        );
    }

    public void changePassword(
        ApiDtos.ChangePasswordRequest request
    ) throws BindException {
        profileCommandService.changePassword(
            request
        );
    }
}
