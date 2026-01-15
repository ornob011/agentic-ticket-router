package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.CustomerTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerTierRepository extends JpaRepository<CustomerTier, Long> {

    List<CustomerTier> findByActiveTrueOrderByDisplayNameAsc();

    boolean existsByCode(String code);

    Optional<CustomerTier> findByCode(String code);
}
