package com.dsi.support.agenticrouter.service.routing.contract;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.RoutingActionParameterKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UseKnowledgeArticleContract implements RouterNextActionContract {

    private final RoutingActionParameterReader routingActionParameterReader;

    @Override
    public NextAction action() {
        return NextAction.USE_KNOWLEDGE_ARTICLE;
    }

    @Override
    public void validate(
        RouterResponse routerResponse
    ) {
        routingActionParameterReader.requirePositiveLong(
            routerResponse.getActionParameters(),
            RoutingActionParameterKey.ARTICLE_ID,
            "article_id must be numeric for USE_KNOWLEDGE_ARTICLE",
            "article_id must be numeric for USE_KNOWLEDGE_ARTICLE",
            "article_id must be positive for USE_KNOWLEDGE_ARTICLE"
        );
    }
}
