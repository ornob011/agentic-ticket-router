package com.dsi.support.agenticrouter.service.common;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.repository.AuditEventRepository;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import org.springframework.stereotype.Service;

@Service
public class AuditEventItemMapper {

    public ApiDtos.AuditEventItem toAuditEventItem(
        AuditEventRepository.AuditEventView auditEvent
    ) {
        return ApiDtos.AuditEventItem.builder()
                                     .id(auditEvent.getId())
                                     .eventType(auditEvent.getEventType())
                                     .eventTypeLabel(EnumDisplayNameResolver.resolve(
                                         auditEvent.getEventType()
                                     ))
                                     .description(auditEvent.getDescription())
                                     .performedBy(auditEvent.getPerformedByName())
                                     .createdAt(auditEvent.getCreatedAt())
                                     .build();
    }
}
