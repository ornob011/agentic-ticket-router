package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.ModelRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModelRegistryRepository extends JpaRepository<ModelRegistry, Long> {

    List<ModelRegistry> findByActiveTrue();

    Optional<ModelRegistry> findByModelTag(String modelTag);
}
