package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.ModelRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelRegistryRepository extends JpaRepository<ModelRegistry, Long> {

    List<ModelRegistry> findByActiveTrue();
}
