package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketMetadataService {

    private final TicketAccessPolicyService ticketAccessPolicyService;

    public ApiDtos.TicketMetadataResponse metadata(
        AppUser user
    ) {
        List<ApiDtos.LookupOption> queueOptions = Arrays.stream(TicketQueue.values())
                                                        .map(ticketQueue -> ApiDtos.LookupOption.builder()
                                                                                                .code(ticketQueue.name())
                                                                                                .name(EnumDisplayNameResolver.resolve(ticketQueue))
                                                                                                .build())
                                                        .toList();

        List<ApiDtos.LookupOption> accessibleQueueOptions = ticketAccessPolicyService.accessibleQueues(user)
                                                                                     .stream()
                                                                                     .map(ticketQueue -> ApiDtos.LookupOption.builder()
                                                                                                                             .code(ticketQueue.name())
                                                                                                                             .name(EnumDisplayNameResolver.resolve(ticketQueue))
                                                                                                                             .build())
                                                                                     .toList();

        List<ApiDtos.LookupOption> statusOptions = Arrays.stream(TicketStatus.values())
                                                         .map(ticketStatus -> ApiDtos.LookupOption.builder()
                                                                                                  .code(ticketStatus.name())
                                                                                                  .name(EnumDisplayNameResolver.resolve(ticketStatus))
                                                                                                  .build())
                                                         .toList();

        List<ApiDtos.LookupOption> priorityOptions = Arrays.stream(TicketPriority.values())
                                                           .map(ticketPriority -> ApiDtos.LookupOption.builder()
                                                                                                      .code(ticketPriority.name())
                                                                                                      .name(EnumDisplayNameResolver.resolve(ticketPriority))
                                                                                                      .build())
                                                           .toList();

        return ApiDtos.TicketMetadataResponse.builder()
                                             .queues(queueOptions)
                                             .accessibleQueues(accessibleQueueOptions)
                                             .statuses(statusOptions)
                                             .priorities(priorityOptions)
                                             .build();
    }
}
