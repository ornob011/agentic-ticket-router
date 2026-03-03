package com.dsi.support.agenticrouter.configuration;

import com.dsi.support.agenticrouter.enums.TicketQueryScope;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TicketQueryScopeConverter implements Converter<String, TicketQueryScope> {

    @Override
    public TicketQueryScope convert(
        @NonNull String source
    ) {
        return TicketQueryScope.from(
            source
        );
    }
}
