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
            customer = new ApiDtos.DashboardCustomerSection(
                customerData.openTickets(),
                customerData.waitingOnMe(),
                customerData.resolvedTickets(),
                customerData.closedTickets()
            );
        }

        ApiDtos.DashboardAgentSection agent = null;
        if (Objects.nonNull(agentData)) {
            agent = new ApiDtos.DashboardAgentSection(
                agentData.myAssignedCount(),
                queueCount(
                    dashboardDto,
                    TicketQueue.BILLING_Q
                ),
                queueCount(
                    dashboardDto,
                    TicketQueue.TECH_Q
                ),
                queueCount(
                    dashboardDto,
                    TicketQueue.OPS_Q
                ),
                queueCount(
                    dashboardDto,
                    TicketQueue.SECURITY_Q
                ),
                queueCount(
                    dashboardDto,
                    TicketQueue.ACCOUNT_Q
                ),
                queueCount(
                    dashboardDto,
                    TicketQueue.GENERAL_Q
                )
            );
        }

        ApiDtos.DashboardSupervisorSection supervisor = null;
        if (Objects.nonNull(supervisorData)) {
            supervisor = new ApiDtos.DashboardSupervisorSection(
                supervisorData.pendingEscalations(),
                supervisorData.slaBreaches(),
                supervisorData.humanReviewCount()
            );
        }

        ApiDtos.DashboardAdminSection admin = null;
        if (Objects.nonNull(adminData)) {
            admin = new ApiDtos.DashboardAdminSection(
                adminData.totalUsers(),
                adminData.totalTickets(),
                adminData.activeModelTag(),
                adminData.routingSuccessRate(),
                adminData.avgRoutingLatency()
            );
        }

        List<ApiDtos.TicketSummary> recentTickets = List.of();
        if (Objects.nonNull(customerData)) {
            recentTickets = customerData.recentTickets()
                                        .stream()
                                        .map(ticketSummary -> new ApiDtos.TicketSummary(
                                            ticketSummary.id(),
                                            ticketSummary.formattedTicketNo(),
                                            ticketSummary.subject(),
                                            ticketSummary.status(),
                                            null,
                                            null,
                                            null,
                                            ticketSummary.lastActivityAt(),
                                            null,
                                            null
                                        ))
                                        .toList();
        }

        return new ApiDtos.DashboardResponse(
            new ApiDtos.UserMe(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
            ),
            customer,
            agent,
            supervisor,
            admin,
            recentTickets
        );
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
