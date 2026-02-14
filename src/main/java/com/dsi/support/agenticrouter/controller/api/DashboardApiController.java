package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.service.dashboard.DashboardPageQueryService;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardPageQueryService dashboardPageQueryService;

    @GetMapping
    public ApiDtos.DashboardResponse getDashboard() {
        Long userId = Utils.getLoggedInUserId();
        DashboardDto dashboardDto = dashboardPageQueryService.loadDashboardViewForUser(userId);
        AppUser user = Utils.getLoggedInUserDetails();
        DashboardDto.CustomerData customerData = dashboardDto.customerData();
        DashboardDto.AgentData agentData = dashboardDto.agentData();
        DashboardDto.SupervisorData supervisorData = dashboardDto.supervisorData();
        DashboardDto.AdminData adminData = dashboardDto.adminData();

        ApiDtos.DashboardCustomerSection customer = null;
        if (Objects.nonNull(customerData)) {
            customer = ApiDtos.DashboardCustomerSection.builder()
                                                       .openTickets(customerData.openTickets())
                                                       .waitingOnMe(customerData.waitingOnMe())
                                                       .resolvedTickets(customerData.resolvedTickets())
                                                       .closedTickets(customerData.closedTickets())
                                                       .build();
        }

        ApiDtos.DashboardAgentSection agent = null;
        if (Objects.nonNull(agentData)) {
            agent = ApiDtos.DashboardAgentSection.builder()
                                                 .myAssignedCount(agentData.myAssignedCount())
                                                 .queueBilling(queueCount(
                                                     dashboardDto,
                                                     TicketQueue.BILLING_Q
                                                 ))
                                                 .queueTech(queueCount(
                                                     dashboardDto,
                                                     TicketQueue.TECH_Q
                                                 ))
                                                 .queueOps(queueCount(
                                                     dashboardDto,
                                                     TicketQueue.OPS_Q
                                                 ))
                                                 .queueSecurity(queueCount(
                                                     dashboardDto,
                                                     TicketQueue.SECURITY_Q
                                                 ))
                                                 .queueAccount(queueCount(
                                                     dashboardDto,
                                                     TicketQueue.ACCOUNT_Q
                                                 ))
                                                 .queueGeneral(queueCount(
                                                     dashboardDto,
                                                     TicketQueue.GENERAL_Q
                                                 ))
                                                 .build();
        }

        ApiDtos.DashboardSupervisorSection supervisor = null;
        if (Objects.nonNull(supervisorData)) {
            supervisor = ApiDtos.DashboardSupervisorSection.builder()
                                                           .pendingEscalations(supervisorData.pendingEscalations())
                                                           .slaBreaches(supervisorData.slaBreaches())
                                                           .humanReviewCount(supervisorData.humanReviewCount())
                                                           .build();
        }

        ApiDtos.DashboardAdminSection admin = null;
        if (Objects.nonNull(adminData)) {
            admin = ApiDtos.DashboardAdminSection.builder()
                                                 .totalUsers(adminData.totalUsers())
                                                 .totalTickets(adminData.totalTickets())
                                                 .activeModelTag(adminData.activeModelTag())
                                                 .routingSuccessRate(adminData.routingSuccessRate())
                                                 .avgRoutingLatency(adminData.avgRoutingLatency())
                                                 .build();
        }

        List<ApiDtos.TicketSummary> recentTickets = List.of();
        if (Objects.nonNull(customerData)) {
            recentTickets = customerData.recentTickets()
                                        .stream()
                                        .map(ticketSummary -> ApiDtos.TicketSummary.builder()
                                                                                   .id(ticketSummary.id())
                                                                                   .formattedTicketNo(ticketSummary.formattedTicketNo())
                                                                                   .subject(ticketSummary.subject())
                                                                                   .status(ticketSummary.status())
                                                                                   .category(null)
                                                                                   .priority(null)
                                                                                   .queue(null)
                                                                                   .lastActivityAt(ticketSummary.lastActivityAt())
                                                                                   .customerName(null)
                                                                                   .assignedAgentName(null)
                                                                                   .build())
                                        .toList();
        }

        ApiDtos.UserMe userMe = ApiDtos.UserMe.builder()
                                              .id(user.getId())
                                              .username(user.getUsername())
                                              .fullName(user.getFullName())
                                              .role(user.getRole())
                                              .build();

        return ApiDtos.DashboardResponse.builder()
                                        .user(userMe)
                                        .customer(customer)
                                        .agent(agent)
                                        .supervisor(supervisor)
                                        .admin(admin)
                                        .recentTickets(recentTickets)
                                        .build();
    }

    private long queueCount(
        DashboardDto dashboardDto,
        TicketQueue ticketQueue
    ) {
        if (Objects.isNull(dashboardDto.agentData()) || Objects.isNull(dashboardDto.agentData().queueCounts())) {
            return 0L;
        }

        return dashboardDto.agentData()
                           .queueCounts()
                           .getOrDefault(
                               ticketQueue,
                               0L
                           );
    }
}
