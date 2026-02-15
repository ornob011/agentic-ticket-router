package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.service.admin.AdminManagementService;
import com.dsi.support.agenticrouter.service.ai.ModelService;
import com.dsi.support.agenticrouter.service.auth.PasswordHashService;
import com.dsi.support.agenticrouter.service.policy.PolicyConfigService;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import com.dsi.support.agenticrouter.util.Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final ModelService modelService;
    private final PolicyConfigService policyConfigService;
    private final PasswordHashService passwordHashService;
    private final AdminManagementService adminManagementService;

    @GetMapping("/model-registry")
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

    @PostMapping("/model-registry/activate")
    public void activateModel(
        @Valid @RequestBody ApiDtos.ActivateModelRequest request
    ) {
        modelService.activateModel(
            StringNormalizationUtils.trimToNull(request.modelTag()),
            Utils.getLoggedInUserId()
        );
    }

    @GetMapping("/policy-config")
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

    @PatchMapping("/policy-config")
    public void updatePolicyConfig(
        @Valid @RequestBody ApiDtos.PolicyUpdateRequest request
    ) {
        policyConfigService.updatePolicy(
            parsePolicyConfigKey(request.configKey()),
            request.configValue()
        );
    }

    @GetMapping("/users")
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

    @PostMapping("/users")
    public void createStaffUser(
        @Valid @RequestBody ApiDtos.StaffCreateRequest request
    ) {
        policyConfigService.createStaffUser(
            request.username(),
            request.email(),
            request.fullName(),
            request.role(),
            passwordHashService.getPasswordHash(request.password())
        );
    }

    @GetMapping("/queue-memberships")
    public List<ApiDtos.QueueMembershipInfo> queueMemberships() {
        return adminManagementService.queueMemberships();
    }

    @PostMapping("/queue-memberships")
    public void createQueueMembership(
        @Valid @RequestBody ApiDtos.QueueMembershipCreateRequest request
    ) {
        adminManagementService.createQueueMembership(
            request
        );
    }

    @DeleteMapping("/queue-memberships/{membershipId}")
    public void deleteQueueMembership(
        @PathVariable Long membershipId
    ) {
        adminManagementService.deleteQueueMembership(
            membershipId
        );
    }

    @GetMapping("/audit-log")
    public ApiDtos.PagedResponse<ApiDtos.AuditEventItem> auditLog(
        @RequestParam(required = false) Long ticketId,
        @RequestParam(required = false) AuditEventType eventType,
        @RequestParam(required = false) Long performedById,
        @RequestParam(required = false) Instant startDate,
        @RequestParam(required = false) Instant endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return adminManagementService.auditLog(
            ticketId,
            eventType,
            performedById,
            startDate,
            endDate,
            page,
            size
        );
    }

    private PolicyConfigKey parsePolicyConfigKey(
        String configKey
    ) {
        return PolicyConfigKey.valueOf(
            StringNormalizationUtils.upperTrimmedOrEmpty(configKey)
        );
    }
}
