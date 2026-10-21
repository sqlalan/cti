package br.com.cti.insights.service;

import br.com.cti.insights.dto.ClienteDTO;
import br.com.cti.insights.dto.UploadLoteDTO;
import br.com.cti.insights.model.LoteValidadoEvent;
import br.com.cti.insights.model.UploadLinha;
import br.com.cti.insights.model.UploadLote;
import br.com.cti.insights.repository.UploadLinhaRepository;
import br.com.cti.insights.repository.UploadLoteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Recebe as linhas que o Pinia aprovou, valida e padroniza de novo (aula 10) e grava o lote em
 * AGUARDANDO_ETL. Depois do commit, o PythonEtlService envia as linhas ao Python.
 */
@Service
public class UploadService {

	private final ClienteValidacaoService validacao;

	private final UploadLoteRepository lotes;

	private final UploadLinhaRepository linhas;

	private final ApplicationEventPublisher eventos;

	public UploadService(ClienteValidacaoService validacao, UploadLoteRepository lotes, UploadLinhaRepository linhas,
			ApplicationEventPublisher eventos) {
		this.validacao = validacao;
		this.lotes = lotes;
		this.linhas = linhas;
		this.eventos = eventos;
	}

	/**
	 * recebidas = linhas da planilha, boas e ruins; as ruins ficaram no navegador e entram so na conta.
	 */
	@Transactional
	public UploadLoteDTO registrar(String arquivo, Integer recebidas, List<ClienteDTO> clientes, String enviadoPor) {
		if (clientes.isEmpty()) {
			throw new IllegalArgumentException("Nenhuma linha válida foi enviada");
		}
		List<ClienteDTO> prontos = validacao.validarEPreparar(clientes);
		int total = recebidas == null ? prontos.size() : Math.max(recebidas, prontos.size());
		String nome = StringUtils.hasText(arquivo) ? StringUtils.getFilename(arquivo.trim()) : "planilha";

		UploadLote lote = lotes.save(new UploadLote(nome, enviadoPor, total, prontos.size()));
		List<UploadLinha> novas = new ArrayList<>();
		for (int i = 0; i < prontos.size(); i++) {
			novas.add(linha(lote, i + 1, prontos.get(i)));
		}
		linhas.saveAll(novas);

		eventos.publishEvent(new LoteValidadoEvent(lote.getId()));
		return UploadLoteDTO.de(lote);
	}

	@Transactional(readOnly = true)
	public List<UploadLoteDTO> listar() {
		return lotes.findAllByOrderByIdDesc().stream().map(UploadLoteDTO::de).toList();
	}

	private static UploadLinha linha(UploadLote lote, int numero, ClienteDTO c) {
		UploadLinha linha = new UploadLinha(lote, numero);
		linha.setCodigoCliente(c.codigoCliente());
		linha.setNomeCliente(c.nomeCliente());
		linha.setConsultor(c.consultor());
		linha.setSegmento(c.segmento());
		linha.setNivelCliente(c.nivelCliente());
		linha.setFaturamentoAnual(BigDecimal.valueOf(c.faturamentoAnual()).setScale(2, RoundingMode.HALF_UP));
		linha.setServicosContratados(c.servicosContratados());
		linha.setDataContratacao(c.dataContratacao());
		linha.setCidade(StringUtils.hasText(c.cidade()) ? c.cidade() : null);
		linha.setUf(StringUtils.hasText(c.uf()) ? c.uf() : null);
		return linha;
	}

}
