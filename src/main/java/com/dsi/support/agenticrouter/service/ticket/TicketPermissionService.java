package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketPermissionService {

    private final TicketAccessPolicyService ticketAccessPolicyService;

    public ApiDtos.TicketPermissions resolvePermissions(
        SupportTicket supportTicket,
        AppUser user
    ) {
        return ApiDtos.TicketPermissions.builder()
                                        .canReply(ticketAccessPolicyService.canReply(
                                            supportTicket,
                                            user
                                        ))
                                        .canChangeStatus(!ticketAccessPolicyService.allowedStatusTransitions(
                                            supportTicket,
                                            user
                                        ).isEmpty())
                                        .canAssignSelf(ticketAccessPolicyService.canAssignSelf(
                                            supportTicket,
                                            user
                                        ))
                                        .canAssignOthers(ticketAccessPolicyService.canAssignOthers(
                                            user
                                        ))
                                        .canOverrideRouting(ticketAccessPolicyService.canOverrideRouting(
                                            user
                                        ))
                                        .canResolveEscalation(ticketAccessPolicyService.canResolveEscalation(
                                            user
                                        ))
                                        .allowedStatusTransitions(
                                            ticketAccessPolicyService.allowedStatusTransitions(
                                                supportTicket,
                                                user
                                            ).stream().toList()
                                        )
                                        .build();
    }
}
