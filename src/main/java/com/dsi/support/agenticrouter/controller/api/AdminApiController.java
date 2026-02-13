package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AuditEvent;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.repository.AuditEventRepository;
import com.dsi.support.agenticrouter.service.ai.ModelService;
import com.dsi.support.agenticrouter.service.auth.PasswordHashService;
import com.dsi.support.agenticrouter.service.policy.PolicyConfigService;
import com.dsi.support.agenticrouter.util.Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final AuditEventRepository auditEventRepository;

    @GetMapping("/model-registry")
    public List<ApiDtos.ModelInfo> modelRegistry() {
        return modelService.getAllModels()
                           .stream()
                           .map(model -> new ApiDtos.ModelInfo(
                                   model.getId(),
                                   model.getModelTag(),
                                   model.getModelName(),
                                   model.isActive(),
                                   Objects.isNull(model.getActivatedBy()) ? null : model.getActivatedBy().getId(),
                                   model.getActivatedAt()
                               )
                           ).toList();
    }

    @PostMapping("/model-registry/activate")
    public void activateModel(
        @Valid @RequestBody ApiDtos.ActivateModelRequest request
    ) {
        modelService.activateModel(
            request.modelTag(),
            Utils.getLoggedInUserId()
        );
    }

    @GetMapping("/policy-config")
    public List<ApiDtos.PolicyInfo> policyConfig() {
        return policyConfigService.getAllActivePolicies()
                                  .stream()
                                  .map(policy -> new ApiDtos.PolicyInfo(
                                          policy.getId(),
                                          policy.getConfigKey().name(),
                                          policy.getConfigValue(),
                                          policy.isActive()
                                      )
                                  ).toList();
    }

    @PatchMapping("/policy-config")
    public void updatePolicyConfig(
        @Valid @RequestBody ApiDtos.PolicyUpdateRequest request
    ) {
        policyConfigService.updatePolicy(
            PolicyConfigKey.valueOf(request.configKey()),
            request.configValue()
        );
    }

    @GetMapping("/users")
    public List<ApiDtos.UserInfo> users() {
        return policyConfigService.getAllUsers()
                                  .stream()
                                  .map(user -> new ApiDtos.UserInfo(
                                          user.getId(),
                                          user.getUsername(),
                                          user.getEmail(),
                                          user.getFullName(),
                                          user.getRole(),
                                          user.isActive()
                                      )
                                  ).toList();
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
        Page<AuditEvent> auditEvents = auditEventRepository.findByFilters(
            ticketId,
            eventType,
            performedById,
            startDate,
            endDate,
            PageRequest.of(
                page,
                size,
                Sort.by("createdAt")
                    .descending()
            )
        );

        return new ApiDtos.PagedResponse<>(
            auditEvents.getContent()
                       .stream()
                       .map(event -> new ApiDtos.AuditEventItem(
                               event.getId(),
                               event.getEventType(),
                               event.getDescription(),
                               Objects.isNull(event.getPerformedBy()) ? "SYSTEM" : event.getPerformedBy().getFullName(),
                               event.getCreatedAt()
                           )
                       )
                       .toList(),
            auditEvents.getNumber(),
            auditEvents.getSize(),
            auditEvents.getTotalElements(),
            auditEvents.getTotalPages(),
            auditEvents.hasNext()
        );
    }
}
