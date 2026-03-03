package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {

    List<Language> findAllByOrderByNameAsc();

    boolean existsByCode(String code);

    Optional<Language> findByCode(String code);
}
