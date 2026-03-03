package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.service.routing.contract.RouterNextActionContract;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class RouterResponseContractValidator {

    private final Map<NextAction, RouterNextActionContract> contractsByAction;

    public RouterResponseContractValidator(
        List<RouterNextActionContract> contracts
    ) {
        this.contractsByAction = createContractMap(
            contracts
        );
    }

    public void validate(
        RouterResponse routerResponse
    ) {
        validateRequiredFields(
            routerResponse
        );
        validateConfidence(
            routerResponse
        );
        validateNextActionContract(
            routerResponse
        );
    }

    private void validateRequiredFields(
        RouterResponse routerResponse
    ) {
        Objects.requireNonNull(routerResponse, "response is required");
        Objects.requireNonNull(routerResponse.getCategory(), "category is required");
        Objects.requireNonNull(routerResponse.getPriority(), "priority is required");
        Objects.requireNonNull(routerResponse.getQueue(), "queue is required");
        Objects.requireNonNull(routerResponse.getNextAction(), "next_action is required");
        Objects.requireNonNull(routerResponse.getConfidence(), "confidence is required");
    }

    private void validateConfidence(
        RouterResponse routerResponse
    ) {
        BigDecimal confidence = routerResponse.getConfidence();

        if (confidence.compareTo(BigDecimal.ZERO) < 0
            || confidence.compareTo(BigDecimal.ONE) > 0
        ) {
            throw new IllegalStateException("Confidence must be between 0 and 1");
        }
    }

    private void validateNextActionContract(
        RouterResponse routerResponse
    ) {
        NextAction nextAction = routerResponse.getNextAction();
        RouterNextActionContract contract = contractsByAction.get(nextAction);
        if (contract == null) {
            return;
        }

        contract.validate(
            routerResponse
        );
    }

    private Map<NextAction, RouterNextActionContract> createContractMap(
        List<RouterNextActionContract> contracts
    ) {
        Map<NextAction, List<RouterNextActionContract>> groupedContracts = contracts.stream()
                                                                                    .collect(Collectors.groupingBy(
                                                                                        RouterNextActionContract::action
                                                                                    ));

        EnumMap<NextAction, RouterNextActionContract> map = new EnumMap<>(NextAction.class);

        for (Map.Entry<NextAction, List<RouterNextActionContract>> entry : groupedContracts.entrySet()) {
            if (entry.getValue().size() > 1) {
                throw new IllegalStateException(
                    "Multiple router response contracts configured for action: " + entry.getKey()
                );
            }

            map.put(
                entry.getKey(),
                entry.getValue().getFirst()
            );
        }

        return map;
    }
}
