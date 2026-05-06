package com.peculytics.categorizationservice.repository;

import com.peculytics.categorizationservice.model.CategorizationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorizationRuleRepository extends JpaRepository<CategorizationRule, Long> {
    List<CategorizationRule> findByActiveTrueOrderByKeywordAsc();
}
