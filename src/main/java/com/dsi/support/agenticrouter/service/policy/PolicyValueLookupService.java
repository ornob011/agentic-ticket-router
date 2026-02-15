package com.dsi.support.agenticrouter.service.policy;

import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.PolicyConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PolicyValueLookupService {

    private final PolicyConfigRepository policyConfigRepository;

    public BigDecimal getRequiredRawValue(
        PolicyConfigKey policyConfigKey
    ) {
        return policyConfigRepository.findByConfigKeyAndActiveTrue(policyConfigKey)
                                     .map(PolicyConfig::getConfigValue)
                                     .orElseThrow(
                                         DataNotFoundException.supplier(
                                             PolicyConfig.class,
                                             policyConfigKey
                                         )
                                     );
    }

    public int getRequiredIntValue(
        PolicyConfigKey policyConfigKey
    ) {
        return getRequiredRawValue(policyConfigKey).intValueExact();
    }

    public BigDecimal getRequiredBigDecimalValue(
        PolicyConfigKey policyConfigKey
    ) {
        return getRequiredRawValue(policyConfigKey);
    }
}
