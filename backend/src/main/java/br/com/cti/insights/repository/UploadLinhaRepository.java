package br.com.cti.insights.repository;

import br.com.cti.insights.model.StatusLinha;
import br.com.cti.insights.model.UploadLinha;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadLinhaRepository extends JpaRepository<UploadLinha, Long> {

	List<UploadLinha> findByLoteIdAndStatusOrderByNumeroLinha(Long loteId, StatusLinha status);

}
