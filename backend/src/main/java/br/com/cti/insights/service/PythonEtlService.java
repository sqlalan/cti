package br.com.cti.insights.service;                        // pacote desta classe

import br.com.cti.insights.config.AppProperties;                                   // import: configurações (endereço e chave do Python)
import br.com.cti.insights.dto.ClienteDTO;                                          // import: uma linha da planilha, já validada
import br.com.cti.insights.exception.ProcessamentoException;                        // import: erro com o código HTTP para a tela
import tools.jackson.databind.JsonNode;                                             // import: JSON já lido (árvore de campos)
import tools.jackson.databind.ObjectMapper;                                         // import: ObjectMapper transforma objetos Java em texto JSON (Jackson 3, o do Spring Boot 4)
import java.io.IOException;                                                         // import: erro de entrada/saída
import java.math.BigDecimal;                                                        // import: número decimal exato (dinheiro)
import java.math.RoundingMode;                                                      // import: regra de arredondamento
import java.net.URI;                                                                // import: URI = endereço da API do Python
import java.net.http.HttpClient;                                                    // import: HttpClient = cliente HTTP do próprio Java (faz o POST)
import java.net.http.HttpRequest;                                                   // import: HttpRequest = o pedido (endereço, cabeçalhos e corpo)
import java.net.http.HttpResponse;                                                  // import: HttpResponse = a resposta do Python
import java.nio.charset.StandardCharsets;                                           // import: UTF-8 para acentos
import java.time.Duration;                                                          // import: Duration = tempo máximo de espera
import java.util.ArrayList;                                                         // import: lista que cresce
import java.util.LinkedHashMap;                                                     // import: mapa que mantém a ordem dos campos
import java.util.List;                                                              // import: tipo lista
import java.util.Map;                                                               // import: tipo mapa (chave -> valor)
import org.slf4j.Logger;                                                            // import: Logger = escreve no terminal
import org.slf4j.LoggerFactory;                                                     // import: fábrica de Logger
import org.springframework.http.HttpStatus;                                         // import: códigos HTTP
import org.springframework.stereotype.Service;                                      // import: marca como serviço do Spring

/**
 * ENVIA as linhas válidas para o serviço Python. O Python cria o lote, limpa, calcula e
 * GRAVA no banco (inclusive a telemetria); o Java só espera a resposta {"ok": true, "loteId": ...}.
 * É um POST HTTP com o JSON em ETL_URL (padrão http://localhost:8000/etl).
 */
@Service                                                                            // serviço do Spring
public class PythonEtlService {                                                     // CHAMA O PYTHON com as linhas aprovadas
    private static final Logger log = LoggerFactory.getLogger(PythonEtlService.class);  // logger desta classe

    // Mensagens para a tela, sem nome de tecnologia; o motivo técnico vai para o log.
    static final String INDISPONIVEL = "O processamento está indisponível no momento. Tente enviar de novo em alguns minutos.";
    static final String FALHOU = "Não foi possível processar esta planilha. Tente enviar de novo em alguns minutos; se continuar, avise o suporte.";

    private final String url;                                                       // endereço da API do Python (ETL_URL)
    private final String chave;                                                     // chave combinada com o Python (ETL_KEY; vazia = sem chave)
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();  // cliente HTTP (espera até 10 s para conectar)
    private final ObjectMapper json;                                                // transforma o pedido em JSON e lê a resposta
    public PythonEtlService(AppProperties props, ObjectMapper json) {                // construtor: o Spring entrega as peças
        this.url = props.etl() == null || props.etl().url() == null || props.etl().url().isBlank()  // endereço não configurado?
                ? "http://localhost:8000/etl" : props.etl().url().trim();           // usa o padrão; senão o configurado
        this.chave = props.etl() == null || props.etl().key() == null ? "" : props.etl().key().trim();  // lê a chave (pode ser vazia)
        this.json = json;                                                           // guarda o conversor de JSON
    }                                                                               // fecha o bloco

    /** Manda as linhas ao Python e devolve o id do lote que ele criou. */
    public Long enviar(String arquivoNome, int recebidas, int invalidas, int comAviso, List<ClienteDTO> linhas) {  // enviar: monta o pedido e espera o Python
        List<Map<String, Object>> validas = new ArrayList<>();                      // lista das linhas válidas
        for (int i = 0; i < linhas.size(); i++) {                                   // para cada linha aprovada
            ClienteDTO c = linhas.get(i);                                           // a linha
            Map<String, Object> m = new LinkedHashMap<>();                          // um mapa com os campos da linha
            m.put("linha", i + 1);                                                  // posição da linha no envio
            m.put("codigo_cliente", c.codigoCliente());                             // código do cliente
            m.put("nome_cliente", c.nomeCliente());                                 // nome do cliente
            m.put("consultor", c.consultor());                                      // consultor
            m.put("segmento", c.segmento());                                        // segmento
            m.put("nivel_cliente", c.nivelCliente());                               // nível A, B ou C
            m.put("faturamento_anual", BigDecimal.valueOf(c.faturamentoAnual()).setScale(2, RoundingMode.HALF_UP));  // faturamento com 2 casas
            m.put("servicos_contratados", c.servicosContratados());                 // serviços separados por ;
            m.put("data_contratacao", c.dataContratacao());                         // data aaaa-mm-dd
            m.put("cidade", c.cidade() == null || c.cidade().isBlank() ? null : c.cidade());  // cidade (opcional)
            m.put("uf", c.uf() == null || c.uf().isBlank() ? null : c.uf());        // UF (opcional)
            validas.add(m);                                                         // coloca na lista
        }                                                                           // fecha o bloco

        Map<String, Object> pedido = new LinkedHashMap<>();                         // o pedido completo que vai ao Python
        pedido.put("arquivoNome", arquivoNome);                                     // guarda o nome do arquivo (ex.: dados.xlsx)
        pedido.put("recebidas", recebidas);                                         // total de linhas da planilha (boas + ruins)
        pedido.put("invalidas", invalidas);                                         // quantas linhas tinham erro (essas NÃO vão)
        pedido.put("comAviso", comAviso);                                           // quantas linhas boas têm algum aviso
        pedido.put("linhas", validas);                                              // a lista com as linhas válidas

        try {                                                                       // tenta enviar
            log.info("Enviando {} linhas de {} ao Python: POST {}", validas.size(), arquivoNome, url);  // escreve no terminal
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))       // prepara um pedido para o endereço do Python (ETL_URL)
                    .timeout(Duration.ofMinutes(5))                                 // espera a resposta por até 5 minutos
                    .header("Content-Type", "application/json; charset=utf-8")      // avisa que o corpo é JSON
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(pedido)));  // transforma o mapa em JSON e envia com o método POST
            if (!chave.isEmpty()) req.header("X-ETL-Key", chave);                   // envia a "senha" combinada entre o Java e o Python
            HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));  // dispara e espera o Python responder
            if (resp.statusCode() != 200) {                                         // o Python respondeu erro?
                throw falha("O serviço Python respondeu " + resp.statusCode() + ": " + resp.body(), HttpStatus.BAD_GATEWAY, FALHOU);  // a tela mostra a mensagem amigável
            }                                                                       // fecha o bloco
            JsonNode corpo = json.readTree(resp.body());                            // lê a resposta {"ok": true, "loteId": 3}
            Long loteId = corpo.path("loteId").asLong();                            // id do lote que o Python criou
            log.info("Lote {} processado pelo Python", loteId);                     // escreve no terminal
            return loteId;                                                          // devolve o id para a tela acompanhar
        } catch (IOException e) {                                                   // não conseguiu falar com o Python (serviço desligado?)
            throw falha("Não foi possível falar com o serviço Python em " + url + " (ele está ligado? uvicorn api:app --port 8000).",
                    HttpStatus.SERVICE_UNAVAILABLE, INDISPONIVEL);                  // 503: serviço indisponível
        } catch (InterruptedException e) {                                          // thread interrompida
            Thread.currentThread().interrupt();                                     // restaura o sinal de interrupção
            throw falha("Envio ao Python interrompido.", HttpStatus.SERVICE_UNAVAILABLE, INDISPONIVEL);  // 503
        }                                                                           // fecha o bloco
    }                                                                               // fecha o bloco

    private ProcessamentoException falha(String motivo, HttpStatus status, String mensagem) {  // falha: registra o motivo técnico e monta o erro da tela
        log.error("ETL de planilha: {}", motivo);                                   // registra no terminal
        return new ProcessamentoException(status, mensagem);                        // erro com a mensagem amigável
    }                                                                               // fecha o bloco
}                                                                                   // fecha o bloco
