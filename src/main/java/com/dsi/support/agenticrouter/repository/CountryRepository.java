package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    List<Country> findByActiveTrueOrderByNameAsc();

    boolean existsByIso2IgnoreCase(String iso2);

    Optional<Country> findByIso2(String iso2);
}
