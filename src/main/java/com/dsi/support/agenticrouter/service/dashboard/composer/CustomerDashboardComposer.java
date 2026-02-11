package com.dsi.support.agenticrouter.service.dashboard.composer;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.service.dashboard.RoleDashboardComposer;
import com.dsi.support.agenticrouter.service.dashboard.section.CustomerDashboardSectionAssembler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomerDashboardComposer implements RoleDashboardComposer {

    private final CustomerDashboardSectionAssembler customerSectionAssembler;

    public CustomerDashboardComposer(CustomerDashboardSectionAssembler customerSectionAssembler) {
        this.customerSectionAssembler = customerSectionAssembler;
    }

    @Override
    public UserRole supportedRole() {
        return UserRole.CUSTOMER;
    }

    @Override
    public DashboardDto composeDashboardViewFor(AppUser dashboardOwner) {
        Long dashboardOwnerId = dashboardOwner.getId();

        log.debug(
            "DashboardComposeCustomer(start) Actor(id:{},role:{})",
            dashboardOwnerId,
            dashboardOwner.getRole()
        );

        DashboardDto dashboardDto = DashboardDto.builder()
                                                .user(dashboardOwner)
                                                .customerData(customerSectionAssembler.buildCustomerSectionFor(dashboardOwnerId))
                                                .build();
        log.debug(
            "DashboardComposeCustomer(complete) Actor(id:{})",
            dashboardOwnerId
        );
        return dashboardDto;
    }
}
