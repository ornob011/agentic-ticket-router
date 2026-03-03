package com.dsi.support.agenticrouter.service.policy;

import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.PolicyConfigRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PolicyValueLookupService {

    private final PolicyConfigRepository policyConfigRepository;
    private final PolicyValueLookupService selfReference;

    public PolicyValueLookupService(
        PolicyConfigRepository policyConfigRepository,
        @Lazy PolicyValueLookupService selfReference
    ) {
        this.policyConfigRepository = policyConfigRepository;
        this.selfReference = selfReference;
    }

    @Cacheable(value = "policyConfig", key = "#policyConfigKey.name()")
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
        return selfReference.getRequiredRawValue(policyConfigKey).intValueExact();
    }

    public BigDecimal getRequiredBigDecimalValue(
        PolicyConfigKey policyConfigKey
    ) {
        return selfReference.getRequiredRawValue(policyConfigKey);
    }
}
