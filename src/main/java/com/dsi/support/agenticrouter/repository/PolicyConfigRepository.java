package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyConfigRepository extends JpaRepository<PolicyConfig, Long> {

    Optional<PolicyConfig> findByConfigKeyAndActiveTrue(
        PolicyConfigKey policyConfigKey
    );
}
