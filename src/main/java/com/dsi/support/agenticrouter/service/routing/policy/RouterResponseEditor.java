package com.dsi.support.agenticrouter.service.routing.policy;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class RouterResponseEditor {

    public RouterResponse mutate(
        RouterResponse source,
        Consumer<RouterResponse.RouterResponseBuilder> editor
    ) {
        RouterResponse.RouterResponseBuilder builder = RouterResponse.builder()
                                                                     .category(source.getCategory())
                                                                     .priority(source.getPriority())
                                                                     .queue(source.getQueue())
                                                                     .nextAction(source.getNextAction())
                                                                     .confidence(source.getConfidence())
                                                                     .clarifyingQuestion(source.getClarifyingQuestion())
                                                                     .draftReply(source.getDraftReply())
                                                                     .rationaleTags(source.getRationaleTags())
                                                                     .actionParameters(source.getActionParameters())
                                                                     .internalNote(source.getInternalNote());

        editor.accept(builder);
        return builder.build();
    }
}
