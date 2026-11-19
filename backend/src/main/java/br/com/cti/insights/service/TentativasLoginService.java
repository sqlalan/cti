package br.com.cti.insights.service;

import br.com.cti.insights.exception.TentativasExcedidasException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Defesa contra forca bruta: depois de N senhas erradas seguidas para o mesmo e-mail, o login fica
 * bloqueado por um tempo. Acertar a senha zera a contagem.
 *
 * Em memoria: vale para uma instancia so do back-end (o caso do deploy no Azure App Service) e zera ao reiniciar.
 */
@Service
public class TentativasLoginService {

	private record Falhas(int quantidade, Instant bloqueadoAte) {
	}

	private final Map<String, Falhas> porEmail = new ConcurrentHashMap<>();

	private final int limite;

	private final Duration bloqueio;

	private final Clock relogio;

	@Autowired
	public TentativasLoginService(@Value("${cti.login.tentativas:5}") int limite,
			@Value("${cti.login.bloqueio:15m}") Duration bloqueio) {
		this(limite, bloqueio, Clock.systemUTC());
	}

	TentativasLoginService(int limite, Duration bloqueio, Clock relogio) {
		this.limite = limite;
		this.bloqueio = bloqueio;
		this.relogio = relogio;
	}

	/**
	 * Chamado antes de conferir a senha: recusa enquanto o e-mail estiver bloqueado.
	 */
	public void conferir(String email) {
		Falhas falhas = porEmail.get(email);
		if (falhas == null || falhas.bloqueadoAte() == null) {
			return;
		}
		Instant agora = relogio.instant();
		if (agora.isBefore(falhas.bloqueadoAte())) {
			long restante = Duration.between(agora, falhas.bloqueadoAte()).toMillis();
			long minutos = Math.max(1, (restante + 59_999) / 60_000); // arredonda para cima
			throw new TentativasExcedidasException(minutos);
		}
		porEmail.remove(email); // o bloqueio venceu: recomeca do zero
	}

	public void registrarFalha(String email) {
		porEmail.compute(email, (chave, atual) -> {
			int quantidade = (atual == null ? 0 : atual.quantidade()) + 1;
			Instant bloqueadoAte = quantidade >= limite ? relogio.instant().plus(bloqueio) : null;
			return new Falhas(quantidade, bloqueadoAte);
		});
	}

	public void registrarSucesso(String email) {
		porEmail.remove(email);
	}

}
