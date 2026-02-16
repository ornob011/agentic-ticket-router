package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.service.admin.AdminManagementService;
import com.dsi.support.agenticrouter.service.admin.AdminPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final AdminPortalService adminPortalService;
    private final AdminManagementService adminManagementService;

    @GetMapping("/model-registry")
    public List<ApiDtos.ModelInfo> modelRegistry() {
        return adminPortalService.modelRegistry();
    }

    @PostMapping("/model-registry/activate")
    public void activateModel(
        @Valid @RequestBody ApiDtos.ActivateModelRequest request
    ) {
        adminPortalService.activateModel(
            request
        );
    }

    @GetMapping("/policy-config")
    public List<ApiDtos.PolicyInfo> policyConfig() {
        return adminPortalService.policyConfig();
    }

    @PatchMapping("/policy-config")
    public void updatePolicyConfig(
        @Valid @RequestBody ApiDtos.PolicyUpdateRequest request
    ) throws BindException {
        adminPortalService.updatePolicyConfig(
            request
        );
    }

    @GetMapping("/users")
    public List<ApiDtos.UserInfo> users() {
        return adminPortalService.users();
    }

    @PostMapping("/users")
    public void createStaffUser(
        @Valid @RequestBody ApiDtos.StaffCreateRequest request
    ) throws BindException {
        adminPortalService.createStaffUser(
            request
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

}
