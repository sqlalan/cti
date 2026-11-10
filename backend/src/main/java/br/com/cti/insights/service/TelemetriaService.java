package br.com.cti.insights.service;

import br.com.cti.insights.model.Telemetria;
import br.com.cti.insights.repository.TelemetriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra os eventos de processamento: cada envio de planilha e cada chamada ao Python.
 *
 * REQUIRES_NEW: o registro de uma falha fica gravado mesmo quando o envio que falhou e desfeito.
 */
@Service
public class TelemetriaService {

	private final TelemetriaRepository repository;

	public TelemetriaService(TelemetriaRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrar(String evento, boolean sucesso, Integer registros, long inicioNanos, String mensagem) {
		long duracaoMs = (System.nanoTime() - inicioNanos) / 1_000_000;
		String texto = mensagem == null ? null : mensagem.substring(0, Math.min(mensagem.length(), 500));
		repository.save(new Telemetria(evento, sucesso, registros, duracaoMs, texto));
	}

}
