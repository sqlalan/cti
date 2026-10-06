package com.synthetix.cti.repository;

import com.synthetix.cti.model.Consultor;
import com.synthetix.cti.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	// IgnoreCase: contas gravadas antes da padronizacao podem ter maiusculas no e-mail.
	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	Optional<Usuario> findByConsultor(Consultor consultor);

}
