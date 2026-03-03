package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.service.common.UserMeMapper;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TicketDtoMapper {

    private final UserMeMapper userMeMapper;

    public ApiDtos.TicketSummary toTicketSummary(
        SupportTicket supportTicket
    ) {
        return ApiDtos.TicketSummary.builder()
                                    .id(supportTicket.getId())
                                    .formattedTicketNo(supportTicket.getFormattedTicketNo())
                                    .subject(supportTicket.getSubject())
                                    .status(supportTicket.getStatus())
                                    .statusLabel(EnumDisplayNameResolver.resolve(
                                        supportTicket.getStatus()
                                    ))
                                    .category(supportTicket.getCurrentCategory())
                                    .categoryLabel(EnumDisplayNameResolver.resolve(
                                        supportTicket.getCurrentCategory()
                                    ))
                                    .priority(supportTicket.getCurrentPriority())
                                    .priorityLabel(EnumDisplayNameResolver.resolve(
                                        supportTicket.getCurrentPriority()
                                    ))
                                    .queue(supportTicket.getAssignedQueue())
                                    .queueLabel(EnumDisplayNameResolver.resolve(
                                        supportTicket.getAssignedQueue()
                                    ))
                                    .lastActivityAt(supportTicket.getLastActivityAt())
                                    .customerName(resolveDisplayName(
                                        supportTicket.getCustomer()
                                    ))
                                    .assignedAgentName(resolveDisplayName(
                                        supportTicket.getAssignedAgent()
                                    ))
                                    .build();
    }

    public ApiDtos.UserMe toUserMe(
        AppUser appUser
    ) {
        return userMeMapper.toUserMe(
            appUser
        );
    }

    public ApiDtos.TicketMessageItem toMessageItem(
        TicketMessage ticketMessage
    ) {
        AppUser author = ticketMessage.getAuthor();

        return ApiDtos.TicketMessageItem.builder()
                                        .id(ticketMessage.getId())
                                        .content(ticketMessage.getContent())
                                        .authorName(Objects.isNull(author) ? "SYSTEM" : resolveDisplayName(author))
                                        .authorRole(Objects.isNull(author) ? null : author.getRole())
                                        .messageKind(ticketMessage.getMessageKind().name())
                                        .visibleToCustomer(ticketMessage.isVisibleToCustomer())
                                        .createdAt(ticketMessage.getCreatedAt())
                                        .build();
    }

    public List<ApiDtos.TicketRoutingItem> toRoutingItems(
        List<TicketRouting> routingHistory
    ) {
        return routingHistory.stream()
                             .map(ticketRouting -> ApiDtos.TicketRoutingItem.builder()
                                                                            .id(ticketRouting.getId())
                                                                            .version(ticketRouting.getVersion())
                                                                            .category(ticketRouting.getCategory())
                                                                            .categoryLabel(EnumDisplayNameResolver.resolve(
                                                                                ticketRouting.getCategory()
                                                                            ))
                                                                            .priority(ticketRouting.getPriority())
                                                                            .priorityLabel(EnumDisplayNameResolver.resolve(
                                                                                ticketRouting.getPriority()
                                                                            ))
                                                                            .queue(ticketRouting.getQueue())
                                                                            .queueLabel(EnumDisplayNameResolver.resolve(
                                                                                ticketRouting.getQueue()
                                                                            ))
                                                                            .nextAction(ticketRouting.getNextAction().name())
                                                                            .nextActionLabel(EnumDisplayNameResolver.resolve(
                                                                                ticketRouting.getNextAction()
                                                                            ))
                                                                            .confidence(ticketRouting.getConfidence())
                                                                            .overridden(ticketRouting.isOverridden())
                                                                            .overrideReason(ticketRouting.getOverrideReason())
                                                                            .createdAt(ticketRouting.getCreatedAt())
                                                                            .build())
                             .toList();
    }

    private String resolveDisplayName(
        AppUser appUser
    ) {
        if (Objects.isNull(appUser)) {
            return null;
        }

        String fullName = StringNormalizationUtils.trimToNull(appUser.getFullName());
        if (Objects.nonNull(fullName)) {
            return fullName;
        }

        return StringNormalizationUtils.trimToNull(appUser.getUsername());
    }
}
