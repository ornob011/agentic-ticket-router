package com.dsi.support.agenticrouter.service.dashboard;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.service.common.UserMeMapper;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DashboardResponseService {

    private final DashboardPageQueryService dashboardPageQueryService;
    private final UserMeMapper userMeMapper;
    private final DashboardResponseMapper dashboardResponseMapper;

    @Transactional(readOnly = true)
    public ApiDtos.DashboardResponse getDashboardResponse() {
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

        Map<TicketQueue, Long> queueCounts = dashboardResponseMapper.resolveQueueCounts(
            agentData
        );

        ApiDtos.UserMe userMe = userMeMapper.toUserMe(
            currentUser
        );

        ApiDtos.DashboardCustomerSection customerSection = dashboardResponseMapper.toCustomerSection(
            customerData
        );

        ApiDtos.DashboardAgentSection agentSection = dashboardResponseMapper.toAgentSection(
            agentData,
            queueCounts
        );

        ApiDtos.DashboardSupervisorSection supervisorSection = dashboardResponseMapper.toSupervisorSection(
            supervisorData
        );

        ApiDtos.DashboardAdminSection adminSection = dashboardResponseMapper.toAdminSection(
            adminData
        );

        List<ApiDtos.TicketSummary> recentTickets = dashboardResponseMapper.resolveRecentTickets(
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
}
