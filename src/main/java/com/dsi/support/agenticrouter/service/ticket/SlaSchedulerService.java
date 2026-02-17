package com.dsi.support.agenticrouter.service.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlaSchedulerService {

    private final CustomerResponseSlaService customerResponseSlaService;
    private final AgentResponseSlaService agentResponseSlaService;
    private final AutoCloseInactivityService autoCloseInactivityService;

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void checkCustomerResponseTimeout() {
        customerResponseSlaService.checkCustomerResponseTimeout();
    }

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void checkAgentSlaBreach() {
        agentResponseSlaService.checkAgentSlaBreach();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkInactivityAutoClose() {
        autoCloseInactivityService.checkInactivityAutoClose();
    }
}
