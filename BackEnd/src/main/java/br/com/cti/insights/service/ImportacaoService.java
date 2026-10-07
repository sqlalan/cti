package br.com.cti.insights.service;

import br.com.cti.insights.analise.ClienteTratado;
import br.com.cti.insights.analise.ResultadoAnalise;
import br.com.cti.insights.model.Analise;
import br.com.cti.insights.model.Cliente;
import br.com.cti.insights.model.Consultor;
import br.com.cti.insights.model.InsightAnalise;
import br.com.cti.insights.model.InsightFaturamento;
import br.com.cti.insights.model.InsightSegmento;
import br.com.cti.insights.model.NivelCliente;
import br.com.cti.insights.repository.AnaliseRepository;
import br.com.cti.insights.repository.ClienteRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava o resultado do Python: clientes (atualiza pelo codigo CTI ou cria), contratos, a analise e
 * os insights. Tudo numa transacao so: se uma linha falhar, nada da planilha fica gravado.
 */
@Service
public class ImportacaoService {

	private final ClienteRepository clienteRepository;

	private final AnaliseRepository analiseRepository;

	private final CatalogoService catalogo;

	public ImportacaoService(ClienteRepository clienteRepository, AnaliseRepository analiseRepository,
			CatalogoService catalogo) {
		this.clienteRepository = clienteRepository;
		this.analiseRepository = analiseRepository;
		this.catalogo = catalogo;
	}

	@Transactional
	public Analise importar(String arquivo, ResultadoAnalise resultado) {
		Analise analise = new Analise(arquivo, resultado.linhasLidas(), resultado.linhasValidas(),
				resultado.linhasRejeitadas(), resultado.avisos(), resultado.indicadores(), resultado.problemas());

		int criados = 0;
		List<Cliente> importados = new ArrayList<>();
		for (ClienteTratado linha : resultado.clientes()) {
			Optional<Cliente> existente = clienteRepository.findByCodigo(linha.codigo());
			if (existente.isEmpty()) {
				criados++;
			}
			importados.add(clienteRepository.save(gravar(existente, linha)));
		}
		analise.registrarImportacao(criados, importados.size() - criados);

		resultado.insights().forEach(i -> analise.adicionarInsight(new InsightAnalise(i.tipo(), i.titulo(), i.texto())));
		resumirPorSegmento(importados).forEach(analise::adicionarInsight);
		resumirPorFaixa(importados).forEach(analise::adicionarInsight);

		return analiseRepository.save(analise);
	}

	/**
	 * Cliente que ja existe tem os dados e os servicos substituidos pelos da planilha; cliente que
	 * nao veio na planilha nao e tocado.
	 */
	private Cliente gravar(Optional<Cliente> existente, ClienteTratado linha) {
		Consultor consultor = catalogo.consultorPorNome(linha.consultor());
		Cliente cliente = existente.orElseGet(() -> new Cliente(linha.codigo(), linha.nome(), consultor));
		if (existente.isPresent()) {
			cliente.setNome(linha.nome());
			cliente.setConsultor(consultor);
			List.copyOf(cliente.getContratos()).forEach(cliente::removerContrato);
		}
		cliente.setSegmento(linha.segmento());
		cliente.setNivel(NivelCliente.valueOf(linha.nivel()));
		cliente.setFaturamentoAnual(linha.faturamento());
		cliente.setCidade(Objects.requireNonNullElse(linha.cidade(), ""));
		cliente.setUf(Objects.requireNonNullElse(linha.uf(), ""));
		for (String servico : linha.servicos()) {
			cliente.contratar(catalogo.servicoPorNome(servico), linha.contratacao());
		}
		return cliente;
	}

	private List<InsightSegmento> resumirPorSegmento(List<Cliente> clientes) {
		return agrupar(clientes, Cliente::getSegmento).entrySet()
			.stream()
			.sorted(Comparator.comparingInt((Map.Entry<String, List<Cliente>> e) -> e.getValue().size()).reversed())
			.map(e -> new InsightSegmento(e.getKey(), e.getValue().size(), somar(e.getValue())))
			.toList();
	}

	private List<InsightFaturamento> resumirPorFaixa(List<Cliente> clientes) {
		// Cliente#calcularFaixaFaturamento decide a faixa; aqui so se conta.
		return agrupar(clientes, Cliente::calcularFaixaFaturamento).entrySet()
			.stream()
			.map(e -> new InsightFaturamento(e.getKey(), e.getValue().size(), somar(e.getValue())))
			.toList();
	}

	private static <K extends Comparable<K>> Map<K, List<Cliente>> agrupar(List<Cliente> clientes,
			Function<Cliente, K> chave) {
		return clientes.stream().collect(Collectors.groupingBy(chave, TreeMap::new, Collectors.toList()));
	}

	private static BigDecimal somar(List<Cliente> clientes) {
		return clientes.stream().map(Cliente::getFaturamentoAnual).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

}
