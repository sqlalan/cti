package br.com.cti.insights.repository;

import br.com.cti.insights.model.StatusLote;
import br.com.cti.insights.model.UploadLote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadLoteRepository extends JpaRepository<UploadLote, Long> {

	List<UploadLote> findAllByOrderByIdDesc();

	Optional<UploadLote> findFirstByOrderByIdDesc();

	List<UploadLote> findTop20ByOrderByIdDesc();

	Optional<UploadLote> findFirstByStatusOrderByIdDesc(StatusLote status);

}
