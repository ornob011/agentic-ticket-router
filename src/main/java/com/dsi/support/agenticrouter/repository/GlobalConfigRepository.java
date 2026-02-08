package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.GlobalConfig;
import com.dsi.support.agenticrouter.enums.GlobalConfigKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalConfigRepository extends JpaRepository<GlobalConfig, Long> {

    Optional<GlobalConfig> findByConfigKey(
        GlobalConfigKey globalConfigKey
    );
}
