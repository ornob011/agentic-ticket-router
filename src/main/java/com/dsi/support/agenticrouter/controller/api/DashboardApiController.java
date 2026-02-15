package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.service.dashboard.DashboardPageQueryService;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardPageQueryService dashboardPageQueryService;

    @GetMapping
    public ApiDtos.DashboardResponse getDashboard() {
        long userId = Utils.getLoggedInUserId();

        DashboardDto dashboardDto = dashboardPageQueryService.loadDashboardViewForUser(
            userId
        );

        AppUser currentUser = resolveCurrentUser(
            dashboardDto
        );

        DashboardDto.CustomerData customerData = dashboardDto.customerData();
        DashboardDto.AgentData agentData = dashboardDto.agentData();
        DashboardDto.SupervisorData supervisorData = dashboardDto.supervisorData();
        DashboardDto.AdminData adminData = dashboardDto.adminData();

        Map<TicketQueue, Long> queueCounts = resolveQueueCounts(
            agentData
        );

        ApiDtos.UserMe userMe = toUserMe(
            currentUser
        );

        ApiDtos.DashboardCustomerSection customerSection = toCustomerSection(
            customerData
        );

        ApiDtos.DashboardAgentSection agentSection = toAgentSection(
            agentData,
            queueCounts
        );

        ApiDtos.DashboardSupervisorSection supervisorSection = toSupervisorSection(
            supervisorData
        );

        ApiDtos.DashboardAdminSection adminSection = toAdminSection(
            adminData
        );

        List<ApiDtos.TicketSummary> recentTickets = resolveRecentTickets(
            customerData,
            agentData
        );

        return ApiDtos.DashboardResponse.builder()
                                        .user(userMe)
                                        .customer(customerSection)
                                        .agent(agentSection)
                                        .supervisor(supervisorSection)
                                        .admin(adminSection)
                                        .recentTickets(recentTickets)
                                        .build();
    }

    private AppUser resolveCurrentUser(
        DashboardDto dashboardDto
    ) {
        return Objects.nonNull(dashboardDto.user())
            ? dashboardDto.user()
            : Utils.getLoggedInUserDetails();
    }

    private ApiDtos.UserMe toUserMe(
        AppUser user
    ) {
        return ApiDtos.UserMe.builder()
                             .id(user.getId())
                             .username(user.getUsername())
                             .email(user.getEmail())
                             .fullName(user.getFullName())
                             .role(user.getRole())
                             .roleLabel(EnumDisplayNameResolver.resolve(user.getRole()))
                             .build();
    }

    private ApiDtos.DashboardCustomerSection toCustomerSection(
        DashboardDto.CustomerData customerData
    ) {
        if (Objects.isNull(customerData)) {
            return null;
        }

        return ApiDtos.DashboardCustomerSection.builder()
                                               .openTickets(customerData.openTickets())
                                               .waitingOnMe(customerData.waitingOnMe())
                                               .resolvedTickets(customerData.resolvedTickets())
                                               .closedTickets(customerData.closedTickets())
                                               .build();
    }

    private ApiDtos.DashboardAgentSection toAgentSection(
        DashboardDto.AgentData agentData,
        Map<TicketQueue, Long> queueCounts
    ) {
        if (Objects.isNull(agentData)) {
            return null;
        }

        return ApiDtos.DashboardAgentSection.builder()
                                            .myAssignedCount(agentData.myAssignedCount())
                                            .queueBilling(queueCount(
                                                queueCounts,
                                                TicketQueue.BILLING_Q
                                            ))
                                            .queueTech(queueCount(
                                                queueCounts,
                                                TicketQueue.TECH_Q
                                            ))
                                            .queueOps(queueCount(
                                                queueCounts,
                                                TicketQueue.OPS_Q
                                            ))
                                            .queueSecurity(queueCount(
                                                queueCounts,
                                                TicketQueue.SECURITY_Q
                                            ))
                                            .queueAccount(queueCount(
                                                queueCounts,
                                                TicketQueue.ACCOUNT_Q
                                            ))
                                            .queueGeneral(queueCount(
                                                queueCounts,
                                                TicketQueue.GENERAL_Q
                                            ))
                                            .build();
    }

    private ApiDtos.DashboardSupervisorSection toSupervisorSection(
        DashboardDto.SupervisorData supervisorData
    ) {
        if (Objects.isNull(supervisorData)) {
            return null;
        }

        return ApiDtos.DashboardSupervisorSection.builder()
                                                 .pendingEscalations(supervisorData.pendingEscalations())
                                                 .slaBreaches(supervisorData.slaBreaches())
                                                 .humanReviewCount(supervisorData.humanReviewCount())
                                                 .build();
    }

    private ApiDtos.DashboardAdminSection toAdminSection(
        DashboardDto.AdminData adminData
    ) {
        if (Objects.isNull(adminData)) {
            return null;
        }

        return ApiDtos.DashboardAdminSection.builder()
                                            .totalUsers(adminData.totalUsers())
                                            .totalTickets(adminData.totalTickets())
                                            .activeModelTag(adminData.activeModelTag())
                                            .routingSuccessRate(adminData.routingSuccessRate())
                                            .avgRoutingLatency(adminData.avgRoutingLatency())
                                            .build();
    }

    private List<ApiDtos.TicketSummary> resolveRecentTickets(
        DashboardDto.CustomerData customerData,
        DashboardDto.AgentData agentData
    ) {
        List<DashboardDto.TicketSummary> recentSource = Optional.ofNullable(customerData)
                                                                .map(DashboardDto.CustomerData::recentTickets)
                                                                .orElseGet(
                                                                    () -> Optional.ofNullable(agentData)
                                                                                  .map(DashboardDto.AgentData::myTickets)
                                                                                  .orElse(List.of())
                                                                );
        return mapDashboardTicketSummaries(
            recentSource
        );
    }

    private List<ApiDtos.TicketSummary> mapDashboardTicketSummaries(
        List<DashboardDto.TicketSummary> ticketSummaries
    ) {
        if (Objects.isNull(ticketSummaries)) {
            return List.of();
        }

        return ticketSummaries.stream()
                              .map(this::toTicketSummary)
                              .toList();
    }

    private ApiDtos.TicketSummary toTicketSummary(
        DashboardDto.TicketSummary ticketSummary
    ) {
        return ApiDtos.TicketSummary.builder()
                                    .id(ticketSummary.id())
                                    .formattedTicketNo(ticketSummary.formattedTicketNo())
                                    .subject(ticketSummary.subject())
                                    .status(ticketSummary.status())
                                    .statusLabel(EnumDisplayNameResolver.resolve(ticketSummary.status()))
                                    .category(null)
                                    .categoryLabel(null)
                                    .priority(null)
                                    .priorityLabel(null)
                                    .queue(null)
                                    .queueLabel(null)
                                    .lastActivityAt(ticketSummary.lastActivityAt())
                                    .customerName(null)
                                    .assignedAgentName(null)
                                    .build();
    }

    private Map<TicketQueue, Long> resolveQueueCounts(
        DashboardDto.AgentData agentData
    ) {
        EnumMap<TicketQueue, Long> normalizedQueueCounts = new EnumMap<>(TicketQueue.class);
        for (TicketQueue ticketQueue : TicketQueue.values()) {
            normalizedQueueCounts.put(
                ticketQueue,
                0L
            );
        }

        if (Objects.nonNull(agentData) && Objects.nonNull(agentData.queueCounts())) {
            normalizedQueueCounts.putAll(agentData.queueCounts());
        }

        return Map.copyOf(normalizedQueueCounts);
    }

    private long queueCount(
        Map<TicketQueue, Long> queueCounts,
        TicketQueue ticketQueue
    ) {
        return queueCounts.getOrDefault(
            ticketQueue,
            0L
        );
    }
}
