package br.com.cti.insights.repository;

import br.com.cti.insights.model.Consultor;
import br.com.cti.insights.model.StatusUsuario;
import br.com.cti.insights.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	// IgnoreCase: contas gravadas antes da padronizacao podem ter maiusculas no e-mail.
	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	Optional<Usuario> findByConsultor(Consultor consultor);

	List<Usuario> findAllByOrderByCriadoEmDesc();

	List<Usuario> findByStatusOrderByCriadoEmDesc(StatusUsuario status);

}
