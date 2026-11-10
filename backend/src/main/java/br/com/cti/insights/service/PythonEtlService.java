package br.com.cti.insights.service;                        // pacote desta classe

import br.com.cti.insights.model.LoteValidadoEvent;
import br.com.cti.insights.model.StatusLinha;
import br.com.cti.insights.model.StatusLote;
import br.com.cti.insights.model.Telemetria;
import br.com.cti.insights.model.UploadLinha;
import br.com.cti.insights.model.UploadLote;
import br.com.cti.insights.repository.UploadLinhaRepository;
import br.com.cti.insights.repository.UploadLoteRepository;
import br.com.cti.insights.config.AppProperties;                                   // import: configurações (endereço e chave do Python)
import tools.jackson.databind.ObjectMapper;                                         // import: ObjectMapper transforma objetos Java em texto JSON (Jackson 3, o do Spring Boot 4)
import java.io.IOException;                                                         // import: erro de entrada/saída
import java.net.URI;                                                            // import: URI = endereço da API do Python
import java.net.http.HttpClient;                                                // import: HttpClient = cliente HTTP do próprio Java (faz o POST)
import java.net.http.HttpRequest;                                               // import: HttpRequest = o pedido (endereço, cabeçalhos e corpo)
import java.net.http.HttpResponse;                                              // import: HttpResponse = a resposta do Python
import java.nio.charset.StandardCharsets;                                           // import: UTF-8 para acentos
import java.time.Duration;                                                       // import: Duration = tempo máximo de espera
import java.util.ArrayList;                                                         // import: lista que cresce
import java.util.LinkedHashMap;                                                     // import: mapa que mantém a ordem dos campos
import java.util.List;                                                              // import: tipo lista
import java.util.Map;                                                               // import: tipo mapa (chave -> valor)
import java.util.concurrent.ExecutorService;                                        // import: ExecutorService = fila de tarefas em segundo plano
import java.util.concurrent.Executors;                                              // import: cria a fila
import org.slf4j.Logger;                                                            // import: Logger = escreve no terminal
import org.slf4j.LoggerFactory;                                                     // import: fábrica de Logger
import org.springframework.stereotype.Component;                                    // import: marca como componente do Spring
import org.springframework.transaction.event.TransactionPhase;                      // import: fase da transação (depois do commit)
import org.springframework.transaction.event.TransactionalEventListener;            // import: escuta um evento só quando a transação termina

/**
 * ENVIA as linhas válidas para o serviço Python. O Python limpa, calcula e GRAVA no Azure.
 * É um POST HTTP com o JSON em ETL_URL (padrão http://localhost:8000/etl). O Python é uma API FastAPI (analytics-python/api.py).
 */
@Component                                                                          // componente do Spring
public class PythonEtlService {                                                           // CHAMA O PYTHON depois que o lote foi salvo no banco
    private static final Logger log = LoggerFactory.getLogger(PythonEtlService.class);    // logger desta classe

    private final String url;                                                       // endereço da API do Python (ETL_URL)
    private final String chave;                                                     // chave combinada com o Python (ETL_KEY; vazia = sem chave)
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();  // cliente HTTP (espera até 10 s para conectar)
    private final UploadLoteRepository lotes;                                       // repositório de lotes (para marcar erro)
    private final UploadLinhaRepository linhas;                                     // repositório das linhas (para buscar as válidas)
    private final ObjectMapper json;                                                // transforma o pedido em JSON
    private final TelemetriaService telemetria;                                     // registra cada chamada ao Python (Plano PI: Telemetria)
    private final ExecutorService fila = Executors.newSingleThreadExecutor(r -> {   // fila de uma thread só: um lote por vez
        Thread t = new Thread(r, "etl-trigger");                                    // cria a thread
        t.setDaemon(true);                                                          // thread de fundo: não impede o servidor de desligar
        return t;                                                                   // devolve a thread
    });                                                                             // fim da definição da fila

    public PythonEtlService(AppProperties props, UploadLoteRepository lotes, UploadLinhaRepository linhas, ObjectMapper json, TelemetriaService telemetria) {  // construtor: o Spring entrega as peças
        this.url = props.etl() == null || props.etl().url() == null || props.etl().url().isBlank()  // endereço não configurado?
                ? "http://localhost:8000/etl" : props.etl().url().trim();           // usa o padrão; senão o configurado
        this.chave = props.etl() == null || props.etl().key() == null ? "" : props.etl().key().trim();  // lê a chave (pode ser vazia)
        this.lotes = lotes;                                                         // guarda o repositório de lotes
        this.linhas = linhas;                                                       // guarda o repositório de linhas
        this.json = json;                                                           // guarda o conversor de JSON
        this.telemetria = telemetria;                                               // guarda o registro de telemetria
    }                                                                               // fecha o bloco

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)              // roda só DEPOIS do commit (o lote já está no banco)
    public void aoValidar(LoteValidadoEvent evento) {                               // recebe o evento com o id do lote
        fila.submit(() -> executar(evento.loteId()));                               // coloca a execução na fila
    }                                                                               // fecha o bloco

    private void executar(Long loteId) {                                            // executar: manda um lote para o Python
        long inicio = System.nanoTime();                                            // início, para a duração na telemetria
        try {                                                                       // tenta executar
            UploadLote lote = lotes.findById(loteId).orElseThrow();                 // busca o lote no banco
            List<Map<String, Object>> validas = new ArrayList<>();                  // lista das linhas válidas
            for (UploadLinha l : linhas.findByLoteIdAndStatusOrderByNumeroLinha(loteId, StatusLinha.VALIDA)) {  // para cada linha VALIDA do lote
                Map<String, Object> m = new LinkedHashMap<>();                      // um mapa com os campos da linha
                m.put("linha", l.getNumeroLinha());                                 // número da linha no Excel
                m.put("codigo_cliente", l.getCodigoCliente());                      // código do cliente
                m.put("nome_cliente", l.getNomeCliente());                          // nome do cliente
                m.put("consultor", l.getConsultor());                               // consultor
                m.put("segmento", l.getSegmento());                                 // segmento
                m.put("nivel_cliente", l.getNivelCliente());                        // nível A, B ou C
                m.put("faturamento_anual", l.getFaturamentoAnual());                // faturamento (texto "1850000.00")
                m.put("servicos_contratados", l.getServicosContratados());          // serviços separados por ;
                m.put("data_contratacao", l.getDataContratacao());                  // data aaaa-mm-dd
                m.put("cidade", l.getCidade());                                     // cidade
                m.put("uf", l.getUf());                                             // UF
                validas.add(m);                                                     // coloca na lista
            }                                                                       // fecha o bloco
            Map<String, Object> pedido = new LinkedHashMap<>();                     // o pedido completo que vai ao Python
            pedido.put("loteId", loteId);                                           // id do lote
            pedido.put("recebidas", lote.getTotalLinhas());                         // total de linhas do arquivo (boas + ruins)
            pedido.put("linhas", validas);                                          // as linhas válidas
            byte[] corpo = json.writeValueAsBytes(pedido);                          // transforma o pedido em JSON (bytes UTF-8)

            log.info("Enviando {} linhas do lote {} ao Python: POST {}", validas.size(), loteId, url);  // escreve no terminal
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))       // monta o pedido para a API do Python
                    .timeout(Duration.ofMinutes(10))                                // espera a resposta por até 10 minutos
                    .header("Content-Type", "application/json; charset=utf-8")      // avisa que o corpo é JSON
                    .POST(HttpRequest.BodyPublishers.ofByteArray(corpo));           // POST com o JSON no corpo
            if (!chave.isEmpty()) req.header("X-ETL-Key", chave);                   // se há chave, envia no cabeçalho
            HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));  // ENVIA e espera o Python terminar
            if (resp.statusCode() != 200) {                                         // o Python respondeu erro?
                marcarErro(loteId, "O serviço Python respondeu " + resp.statusCode() + ": " + resp.body(), inicio);  // marca o lote com ERRO e mostra o motivo
            } else {                                                                // o Python respondeu 200: gravou no banco
                telemetria.registrar(Telemetria.EXECUCAO_SCRIPT, true, validas.size(), inicio, "Lote " + loteId + " processado pelo Python");  // registra a execução que deu certo
            }                                                                       // fecha o bloco
        } catch (IOException e) {                                                   // não conseguiu falar com o Python (serviço desligado?)
            marcarErro(loteId, "Não foi possível falar com o serviço Python em " + url + " (ele está ligado? uvicorn api:app --port 8000).", inicio);  // marca o lote com ERRO e explica o motivo
        } catch (InterruptedException e) {                                          // thread interrompida
            Thread.currentThread().interrupt();                                     // restaura o sinal de interrupção
        } catch (RuntimeException e) {                                              // qualquer outro erro inesperado
            marcarErro(loteId, "Erro ao enviar o lote ao Python: " + e.getMessage(), inicio);  // marca o lote com ERRO
        }                                                                           // fecha o bloco
    }                                                                               // fecha o bloco

    private void marcarErro(Long loteId, String mensagem, long inicio) {            // marcarErro: grava ERRO no lote para a tela mostrar
        log.error("ETL do lote {}: {}", loteId, mensagem);                          // registra no terminal
        telemetria.registrar(Telemetria.EXECUCAO_SCRIPT, false, null, inicio, "Lote " + loteId + ": " + mensagem);  // registra a execução que falhou
        lotes.findById(loteId).ifPresent(l -> {                                     // busca o lote no banco
            if (l.getStatus() == StatusLote.PROCESSADO) return;                     // já terminou bem: não sobrescreve
            l.setStatus(StatusLote.ERRO);                                           // status ERRO
            l.setMensagem(mensagem.length() > 480 ? mensagem.substring(0, 480) : mensagem);  // mensagem (máximo 480 caracteres)
            lotes.save(l);                                                          // salva
        });                                                                         // fecha o bloco
    }                                                                               // fecha o bloco
}                                                                                   // fecha o bloco
