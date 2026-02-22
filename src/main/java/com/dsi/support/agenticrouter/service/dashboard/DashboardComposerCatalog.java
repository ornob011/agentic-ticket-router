package com.dsi.support.agenticrouter.service.dashboard;

import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public final class DashboardComposerCatalog {

    private final Map<UserRole, RoleDashboardComposer> composerByRole;

    public DashboardComposerCatalog(
        List<RoleDashboardComposer> discoveredComposers
    ) {
        EnumMap<UserRole, RoleDashboardComposer> composerEnumMap = new EnumMap<>(UserRole.class);

        for (RoleDashboardComposer roleDashboardComposer : discoveredComposers) {
            UserRole supportedRole = roleDashboardComposer.supportedRole();

            RoleDashboardComposer previous = composerEnumMap.putIfAbsent(
                supportedRole,
                roleDashboardComposer
            );

            if (Objects.nonNull(previous)) {
                throw new IllegalStateException(
                    String.format(
                        "Found duplicate dashboard composers for role %s : %s and %s",
                        supportedRole,
                        previous.getClass().getName(),
                        roleDashboardComposer.getClass().getName()
                    )
                );
            }
        }

        this.composerByRole = Map.copyOf(composerEnumMap);
    }

    public RoleDashboardComposer composerSupporting(
        UserRole role
    ) {
        log.debug(
            "DashboardComposerResolve({}) Outcome(role:{})",
            OperationalLogContext.PHASE_START,
            role
        );

        RoleDashboardComposer composer = composerByRole.get(role);

        if (Objects.isNull(composer)) {
            throw new IllegalStateException(
                "No dashboard composer found for role " + role
            );
        }

        log.debug(
            "DashboardComposerResolve({}) Outcome(role:{},composer:{})",
            OperationalLogContext.PHASE_COMPLETE,
            role,
            composer.getClass().getSimpleName()
        );

        return composer;
    }
}
