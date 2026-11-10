package br.com.cti.insights.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cti.insights.exception.TentativasExcedidasException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TentativasLoginServiceTests {

	// Relogio que o teste adianta a mao.
	private static final class RelogioDeTeste extends Clock {

		private Instant agora = Instant.parse("2026-01-01T10:00:00Z");

		void avancar(Duration tempo) {
			agora = agora.plus(tempo);
		}

		@Override
		public Instant instant() {
			return agora;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

	}

	@Test
	void bloqueiaNaQuintaFalhaELiberaDepoisDoTempo() {
		RelogioDeTeste relogio = new RelogioDeTeste();
		TentativasLoginService tentativas = new TentativasLoginService(5, Duration.ofMinutes(15), relogio);

		for (int i = 0; i < 4; i++) {
			tentativas.registrarFalha("a@exemplo.com");
		}
		assertThatCode(() -> tentativas.conferir("a@exemplo.com")).doesNotThrowAnyException();

		tentativas.registrarFalha("a@exemplo.com");
		assertThatThrownBy(() -> tentativas.conferir("a@exemplo.com"))
			.isInstanceOf(TentativasExcedidasException.class)
			.hasMessage("Muitas tentativas de login. Tente de novo em 15 minutos");

		// Outro e-mail nao e afetado.
		assertThatCode(() -> tentativas.conferir("b@exemplo.com")).doesNotThrowAnyException();

		relogio.avancar(Duration.ofMinutes(14).plusSeconds(30));
		assertThatThrownBy(() -> tentativas.conferir("a@exemplo.com"))
			.hasMessage("Muitas tentativas de login. Tente de novo em 1 minuto");

		relogio.avancar(Duration.ofSeconds(30));
		assertThatCode(() -> tentativas.conferir("a@exemplo.com")).doesNotThrowAnyException();
	}

}
