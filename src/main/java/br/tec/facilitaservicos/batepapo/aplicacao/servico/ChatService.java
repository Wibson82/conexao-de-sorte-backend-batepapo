package br.tec.facilitaservicos.batepapo.aplicacao.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDtoSimples;
import br.tec.facilitaservicos.batepapo.dominio.entidade.MensagemSimplesR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioMensagemSimplesR2dbc;
import br.tec.facilitaservicos.batepapo.infraestrutura.cliente.AuthServiceClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Serviço simplificado de Chat - Versão Mínima Viável
 * 
 * Responsabilidades APENAS:
 * - Enviar mensagens
 * - Receber mensagens  
 * - Listar histórico de mensagens
 * - Integração básica com Auth Service
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final RepositorioMensagemSimplesR2dbc repositorio;
    private final AuthServiceClient authServiceClient;

    public ChatService(RepositorioMensagemSimplesR2dbc repositorio, AuthServiceClient authServiceClient) {
        this.repositorio = repositorio;
        this.authServiceClient = authServiceClient;
    }

    /**
     * Envia uma mensagem de chat
     */
    public Mono<MensagemDtoSimples> enviarMensagem(MensagemDtoSimples mensagem) {
        logger.debug("💬 Enviando mensagem: usuário={}, sala={}", mensagem.usuarioId(), mensagem.sala());

        return verificarUsuarioOnline(mensagem.usuarioId())
            .filter(online -> online)
            .switchIfEmpty(Mono.error(new RuntimeException("Usuário offline ou inválido")))
            .then(salvarMensagem(mensagem))
            .map(this::converterParaDto)
            .doOnSuccess(msg -> logger.info("✅ Mensagem enviada: id={}", msg.id()))
            .doOnError(error -> logger.error("❌ Erro ao enviar mensagem: {}", error.getMessage()));
    }

    /**
     * Lista mensagens de uma sala
     */
    public Flux<MensagemDtoSimples> listarMensagens(String sala, int limite) {
        logger.debug("📜 Listando mensagens da sala: {}, limite: {}", sala, limite);

        return repositorio.findBySalaOrderByDataEnvioDesc(sala, limite)
            .map(this::converterParaDto)
            .doOnComplete(() -> logger.debug("✅ Mensagens listadas para sala: {}", sala));
    }

    /**
     * Lista mensagens recentes de uma sala (últimas 50)
     */
    public Flux<MensagemDtoSimples> listarMensagensRecentes(String sala) {
        return listarMensagens(sala, 50);
    }

    /**
     * Conta total de mensagens de uma sala
     */
    public Mono<Long> contarMensagens(String sala) {
        return repositorio.countBySala(sala);
    }

    /**
     * Verifica se usuário está online via Auth Service
     */
    private Mono<Boolean> verificarUsuarioOnline(Long usuarioId) {
        return authServiceClient.isUserOnline(usuarioId)
            .doOnNext(online -> logger.debug("🔍 Usuário {} está {}", usuarioId, online ? "online" : "offline"));
    }

    /**
     * Salva mensagem no banco de dados
     */
    private Mono<MensagemSimplesR2dbc> salvarMensagem(MensagemDtoSimples dto) {
        MensagemSimplesR2dbc entidade = MensagemSimplesR2dbc.nova(
            dto.conteudo(), 
            dto.usuarioId(), 
            dto.usuarioNome(), 
            dto.sala()
        );

        return repositorio.save(entidade);
    }

    /**
     * Converte entidade para DTO
     */
    private MensagemDtoSimples converterParaDto(MensagemSimplesR2dbc entidade) {
        return new MensagemDtoSimples(
            entidade.getId(),
            entidade.getConteudo(),
            entidade.getUsuarioId(),
            entidade.getUsuarioNome(),
            entidade.getSala(),
            entidade.getTipo(),
            entidade.getStatus(),
            entidade.getDataEnvio()
        );
    }
}