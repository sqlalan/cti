package br.com.cti.insights.repository;

import br.com.cti.insights.model.Insight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightRepository extends JpaRepository<Insight, Long> {

}
