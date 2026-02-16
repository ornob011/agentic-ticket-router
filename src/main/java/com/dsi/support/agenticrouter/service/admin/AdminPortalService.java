package com.dsi.support.agenticrouter.service.admin;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.service.ai.ModelService;
import com.dsi.support.agenticrouter.service.auth.PasswordHashService;
import com.dsi.support.agenticrouter.service.policy.PolicyConfigService;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminPortalService {

    private final ModelService modelService;
    private final PolicyConfigService policyConfigService;
    private final PasswordHashService passwordHashService;

    @Transactional(readOnly = true)
    public List<ApiDtos.ModelInfo> modelRegistry() {
        return modelService.getAllModels()
                           .stream()
                           .map(model -> ApiDtos.ModelInfo.builder()
                                                          .id(model.getId())
                                                          .modelTag(model.getModelTag())
                                                          .provider(model.getModelName())
                                                          .active(model.isActive())
                                                          .activatedBy(
                                                              Objects.isNull(model.getActivatedBy())
                                                                  ? null
                                                                  : model.getActivatedBy().getId()
                                                          )
                                                          .activatedAt(model.getActivatedAt())
                                                          .build())
                           .toList();
    }

    @Transactional
    public void activateModel(
        ApiDtos.ActivateModelRequest request
    ) {
        modelService.activateModel(
            StringNormalizationUtils.trimToNull(request.modelTag()),
            Utils.getLoggedInUserId()
        );
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.PolicyInfo> policyConfig() {
        return policyConfigService.getAllActivePolicies()
                                  .stream()
                                  .map(policy -> ApiDtos.PolicyInfo.builder()
                                                                   .id(policy.getId())
                                                                   .configKey(policy.getConfigKey().name())
                                                                   .configValue(policy.getConfigValue())
                                                                   .active(policy.isActive())
                                                                   .build())
                                  .toList();
    }

    @Transactional
    public void updatePolicyConfig(
        ApiDtos.PolicyUpdateRequest request
    ) throws BindException {
        policyConfigService.updatePolicy(
            PolicyConfigKey.valueOf(
                StringNormalizationUtils.upperTrimmedOrEmpty(request.configKey())
            ),
            request.configValue()
        );
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.UserInfo> users() {
        return policyConfigService.getAllUsers()
                                  .stream()
                                  .map(user -> ApiDtos.UserInfo.builder()
                                                               .id(user.getId())
                                                               .username(user.getUsername())
                                                               .email(user.getEmail())
                                                               .fullName(user.getFullName())
                                                               .role(user.getRole())
                                                               .roleLabel(EnumDisplayNameResolver.resolve(
                                                                   user.getRole()
                                                               ))
                                                               .active(user.isActive())
                                                               .build())
                                  .toList();
    }

    @Transactional
    public void createStaffUser(
        ApiDtos.StaffCreateRequest request
    ) throws BindException {
        policyConfigService.createStaffUser(
            request.username(),
            request.email(),
            request.fullName(),
            request.role(),
            passwordHashService.getPasswordHash(request.password())
        );
    }
}
